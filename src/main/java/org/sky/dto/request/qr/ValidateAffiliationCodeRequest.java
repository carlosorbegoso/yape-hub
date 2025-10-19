package org.sky.dto.request.qr;

import jakarta.validation.constraints.NotBlank;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record ValidateAffiliationCodeRequest(
    @NotBlank(message = "Affiliation code is required")
    String affiliationCode
) {}
