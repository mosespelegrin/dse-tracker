package com.moses.dse_track.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    @Email(message = "Enter a valid email")
    @NotBlank(message = "Email is required")
    private String email;
}
