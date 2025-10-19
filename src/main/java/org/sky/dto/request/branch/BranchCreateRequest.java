package org.sky.dto.request.branch;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record BranchCreateRequest(
    @NotBlank(message = "El nombre de la sucursal es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    String name,
    
    @NotBlank(message = "El código de la sucursal es obligatorio")
    @Size(min = 2, max = 20, message = "El código debe tener entre 2 y 20 caracteres")
    String code,
    
    @NotBlank(message = "La dirección de la sucursal es obligatoria")
    @Size(min = 5, max = 200, message = "La dirección debe tener entre 5 y 200 caracteres")
    String address
) {}
