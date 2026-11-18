package com.lastly.lab.dto;
import jakarta.validation.constraints.*;
public record RegisterRequest(@NotBlank @Email String email,
                              @NotBlank @Size(min=12,max=72) String password) {}
