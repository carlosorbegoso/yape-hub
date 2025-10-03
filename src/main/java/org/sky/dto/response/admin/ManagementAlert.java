package org.sky.dto.response.admin;

import java.util.List;

public record ManagementAlert(
    String type,
    String severity,
    String message,
    String affectedBranch,
    List<String> affectedSellers,
    String recommendation
) {}