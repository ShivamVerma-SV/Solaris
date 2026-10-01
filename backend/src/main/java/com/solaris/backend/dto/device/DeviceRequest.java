package com.solaris.backend.dto.device;

import com.solaris.backend.entity.DeviceStatus;
import com.solaris.backend.entity.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class DeviceRequest {
    @NotBlank
    @Size(max = 100)
    private String identifier;

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotNull
    private DeviceType type;

    @NotNull
    private DeviceStatus status;

    @NotNull
    @Positive
    private Long siteId;

    @Size(max = 100)
    private String manufacturer;

    @Size(max = 100)
    private String model;

    @Size(max = 100)
    private String firmwareVersion;

    private Instant lastSeenAt;
}
