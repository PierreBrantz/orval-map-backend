package com.orvalmap.service;

import com.orvalmap.model.PlaceRequestStatus;
import com.orvalmap.model.PlaceVisit;
import com.orvalmap.model.User;
import com.orvalmap.model.PassportDTO;
import com.orvalmap.repository.PlaceRequestRepository;
import com.orvalmap.repository.PlaceVisitRepository;
import com.orvalmap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PassportService {

    private final UserRepository userRepository;
    private final PlaceVisitRepository placeVisitRepository;
    private final PlaceRequestRepository placeRequestRepository;

    public PassportDTO getPassportForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        List<PlaceVisit> visits = getVisitsForUser(username);
        long visitedPlaces = visits.size();
        Set<String> visitedCities = visits.stream()
                .map(visit -> visit.getPlace().getCity())
                .collect(Collectors.toSet());
        long visitedCitiesCount = visitedCities.size();

        long totalSuggestions = placeRequestRepository.countByRequester(user);
        long approvedSuggestions = placeRequestRepository.countByRequesterAndStatus(user, PlaceRequestStatus.APPROVED);
        long pendingSuggestions = placeRequestRepository.countByRequesterAndStatus(user, PlaceRequestStatus.PENDING);

        PassportDTO.SuggestionsStats suggestionsStats = PassportDTO.SuggestionsStats.builder()
                .total(totalSuggestions)
                .approved(approvedSuggestions)
                .pending(pendingSuggestions)
                .build();

        List<PassportDTO.Badge> badges = calculateBadges(visitedPlaces, approvedSuggestions);
        PassportDTO.NextGoal nextGoal = findNextGoal(badges);

        return PassportDTO.builder()
                .visitedPlaces(visitedPlaces)
                .visitedCities(visitedCitiesCount)
                .suggestions(suggestionsStats)
                .badges(badges)
                .nextGoal(nextGoal)
                .build();
    }

    public List<PlaceVisit> getVisitsForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return placeVisitRepository.findByUser(user);
    }

    private List<PassportDTO.Badge> calculateBadges(long visitedPlaces, long approvedSuggestions) {
        List<PassportDTO.Badge> badges = new ArrayList<>();

        badges.add(createBadge("FIRST_DISCOVERY", "Première découverte", visitedPlaces >= 1, visitedPlaces, 1));
        badges.add(createBadge("EXPLORER", "Explorateur", visitedPlaces >= 5, visitedPlaces, 5));
        badges.add(createBadge("ADVENTURER", "Aventurier", visitedPlaces >= 10, visitedPlaces, 10));
        badges.add(createBadge("CONNOISSEUR", "Connaisseur", visitedPlaces >= 25, visitedPlaces, 25));
        badges.add(createBadge("GRAND_EXPLORER", "Grand explorateur", visitedPlaces >= 50, visitedPlaces, 50));

        badges.add(createBadge("SCOUT", "Éclaireur", approvedSuggestions >= 1, approvedSuggestions, 1));
        badges.add(createBadge("CARTOGRAPHER", "Cartographe", approvedSuggestions >= 5, approvedSuggestions, 5));

        return badges;
    }

    private PassportDTO.Badge createBadge(String code, String name, boolean unlocked, long current, long target) {
        return PassportDTO.Badge.builder()
                .code(code)
                .name(name)
                .unlocked(unlocked)
                .current(current)
                .target(target)
                .build();
    }

    private PassportDTO.NextGoal findNextGoal(List<PassportDTO.Badge> badges) {
        return badges.stream()
                .filter(badge -> !badge.isUnlocked())
                .min((b1, b2) -> Long.compare(b1.getTarget(), b2.getTarget()))
                .map(badge -> PassportDTO.NextGoal.builder()
                        .name(badge.getName())
                        .current(badge.getCurrent())
                        .target(badge.getTarget())
                        .build())
                .orElse(null);
    }
}
