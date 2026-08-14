package com.orvalmap.model;

import lombok.Data;

@Data
public class VersionDTO {
    private AndroidVersion android;
    // On peut déjà prévoir la version iOS
    // private IosVersion ios;

    @Data
    public static class AndroidVersion {
        private String latestVersion;
        private String minimumVersion;
    }

    // @Data
    // public static class IosVersion {
    //     private String latestVersion;
    //     private String minimumVersion;
    // }
}
