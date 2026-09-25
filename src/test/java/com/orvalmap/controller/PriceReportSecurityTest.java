package com.orvalmap.controller;
import com.orvalmap.service.PriceReportService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class PriceReportSecurityTest {
    @Configuration @EnableMethodSecurity
    static class Config {
        @Bean PriceReportService service() { return mock(PriceReportService.class); }
        @Bean PriceReportController controller(PriceReportService service) { return new PriceReportController(service); }
    }
    @Test void onlyAdminsCanReadOrDecideReports() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var controller = context.getBean(PriceReportController.class);
            var service = context.getBean(PriceReportService.class);
            SecurityContextHolder.clearContext();
            assertThatThrownBy(controller::pending).isInstanceOf(org.springframework.security.core.AuthenticationException.class);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("alice", "", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
            assertThatThrownBy(controller::pending).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
            assertThatThrownBy(() -> controller.approve(1L)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
            assertThatThrownBy(() -> controller.reject(1L)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
            verifyNoInteractions(service);
            for (String role : List.of("ADMIN", "ROLE_ADMIN")) {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", "", List.of(new SimpleGrantedAuthority(role))));
                controller.pending(); controller.approve(1L); controller.reject(2L);
            }
            verify(service, times(2)).decide(1L, true);
            verify(service, times(2)).decide(2L, false);
        } finally { SecurityContextHolder.clearContext(); }
    }
}
