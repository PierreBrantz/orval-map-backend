package com.orvalmap.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VisitRequestDTO {
    @NotNull(message = "La latitude est obligatoire")
    private Double latitude;

    @NotNull(message = "La longitude est obligatoire")
    private Double longitude;
}
