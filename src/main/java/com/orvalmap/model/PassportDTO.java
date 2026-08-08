package com.orvalmap.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PassportDTO {
    private long visitedPlaces;
    private long visitedCities;
    private SuggestionsStats suggestions;
    private List<Badge> badges;
    private NextGoal nextGoal;

    @Data
    @Builder
    public static class SuggestionsStats {
        private long total;
        private long approved;
        private long pending;
    }

    @Data
    @Builder
    public static class Badge {
        private String code;
        private String name;
        private boolean unlocked;
        private long current;
        private long target;
    }

    @Data
    @Builder
    public static class NextGoal {
        private String name;
        private long current;
        private long target;
    }
}
