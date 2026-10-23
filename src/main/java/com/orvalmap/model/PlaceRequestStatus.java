package com.orvalmap.model;

public enum PlaceRequestStatus {
    /** Suggestion en attente de moderation. */
    PENDING,
    /** Suggestion validee et ajoutee aux lieux publies. */
    APPROVED,
    /** Suggestion refusee lors de la moderation. */
    REJECTED
}
