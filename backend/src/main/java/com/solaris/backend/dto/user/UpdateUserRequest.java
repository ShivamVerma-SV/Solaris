package com.solaris.backend.dto.user;

import com.solaris.backend.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {

    @NotBlank
    @Size(min = 2, max = 120)
    private String name;

    @Email
    @NotBlank
    @Size(max = 254)
    private String email;

    @NotNull
    private UserRole role;

    @Size(min = 8, max = 72)
    private String password;

    @Size(max = 30)
    private String phone;

    private boolean enabled;
}
