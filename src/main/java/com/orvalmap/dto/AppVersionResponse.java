package com.orvalmap.dto;

public record AppVersionResponse(
        String latestVersion,
        String minimumVersion,
        String playStoreUrl
) {
}
