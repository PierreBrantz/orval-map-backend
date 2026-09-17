package com.orvalmap.exception;

import lombok.Getter;

@Getter
public class DuplicatePlaceException extends RuntimeException {

    private final String duplicateType;
    private final Long duplicateId;

    public DuplicatePlaceException(String message, String duplicateType, Long duplicateId) {
        super(message);
        this.duplicateType = duplicateType;
        this.duplicateId = duplicateId;
    }
}
