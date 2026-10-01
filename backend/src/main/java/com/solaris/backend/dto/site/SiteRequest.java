package com.solaris.backend.dto.site;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SiteRequest {
    @NotNull
    @Positive
    private Long ownerId;

    @NotBlank
    @Size(max = 60)
    private String code;

    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 500)
    private String address;

    @NotNull
    @Positive
    private BigDecimal capacityKw;

    private boolean active = true;
}
