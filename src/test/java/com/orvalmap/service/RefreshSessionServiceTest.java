package com.orvalmap.service;
import com.orvalmap.model.*;
import com.orvalmap.repository.*;
import com.orvalmap.security.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class RefreshSessionServiceTest {
    RefreshSessionRepository sessions = mock(RefreshSessionRepository.class);
    UserRepository users = mock(UserRepository.class);
    JwtUtil jwt = mock(JwtUtil.class);
    RefreshSessionService service = new RefreshSessionService(sessions, users, jwt);
    User user;
    @BeforeEach void setup() {
        user=User.builder().id(7L).username("test").password("encoded-password").build();
        when(jwt.generateToken(any(UserDetailsImpl.class))).thenReturn("access");
    }
    RefreshSession capture(String credential) {
        var captor=org.mockito.ArgumentCaptor.forClass(RefreshSession.class);
        verify(sessions).save(captor.capture());
        var session=captor.getValue();
        when(sessions.findLockedByHash(RefreshSessionService.hash(credential))).thenReturn(Optional.of(session));
        when(users.findById(7L)).thenReturn(Optional.of(user));
        return session;
    }
    @Test void issuesRandomHashedCredentialAndRenews() {
        var tokens=service.issue(user);assertEquals(43,tokens.refreshToken().length());
        var session=capture(tokens.refreshToken());
        assertNotEquals(tokens.refreshToken(),session.getTokenHash());
        assertTrue(session.getExpiresAt().isAfter(Instant.now().plus(Duration.ofDays(89))));
        session.setExpiresAt(Instant.now().plusSeconds(60));
        assertEquals("access",service.refresh(tokens.refreshToken()).token());
        assertTrue(session.getExpiresAt().isAfter(Instant.now().plus(Duration.ofDays(89))));
    }
    @Test void expiredSessionIsRejected() {
        var tokens=service.issue(user);capture(tokens.refreshToken()).setExpiresAt(Instant.now().minusSeconds(1));
        assertThrows(ResponseStatusException.class,()->service.refresh(tokens.refreshToken()));
    }
    @Test void passwordChangeInvalidatesSession() {
        var tokens=service.issue(user);capture(tokens.refreshToken());user.setPassword("changed");
        assertThrows(ResponseStatusException.class,()->service.refresh(tokens.refreshToken()));
    }
    @Test void deletedAccountCannotRefresh() {
        var tokens=service.issue(user);capture(tokens.refreshToken());when(users.findById(7L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class,()->service.refresh(tokens.refreshToken()));
    }
    @Test void logoutDeletesSessionAndUnknownTokensAreRejected() {
        var tokens=service.issue(user);var session=capture(tokens.refreshToken());
        service.revoke(tokens.refreshToken());verify(sessions).delete(session);
        assertThrows(ResponseStatusException.class,()->service.refresh("bad"));
    }
}
