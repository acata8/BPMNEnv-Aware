package org.unicam.intermediate.models.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class GpsRequest {
    @NotBlank(message = "User ID required")
    private String userId;
    
    @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    private double lat;
    
    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    private double lon;
}