package org.sky.dto.request.branch;

import jakarta.validation.constraints.Size;

public record BranchUpdateRequest(
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    String name,
    
    @Size(min = 2, max = 20, message = "El código debe tener entre 2 y 20 caracteres")
    String code,
    
    @Size(min = 5, max = 200, message = "La dirección debe tener entre 5 y 200 caracteres")
    String address,
    
    Boolean isActive
) {}
