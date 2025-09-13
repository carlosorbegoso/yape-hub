package org.sky.dto.qr;

import jakarta.validation.constraints.NotBlank;

public record ValidateAffiliationCodeRequest(
    @NotBlank(message = "Affiliation code is required")
    String affiliationCode
) {}
