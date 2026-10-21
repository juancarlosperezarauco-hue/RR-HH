package com.hrcoresystem.hrcore_api.modules.platform.enterprise.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnterpriseInquiryRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
    String fullName,

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 180, message = "El correo no puede superar 180 caracteres")
    String email,

    @NotBlank(message = "La empresa es obligatoria")
    @Size(max = 160, message = "La empresa no puede superar 160 caracteres")
    String company,

    @NotBlank(message = "El tamaño del equipo es obligatorio")
    @Size(max = 80, message = "El tamaño del equipo no puede superar 80 caracteres")
    String teamSize,

    @NotBlank(message = "El mensaje es obligatorio")
    @Size(min = 20, max = 2000, message = "El mensaje debe tener entre 20 y 2000 caracteres")
    String message) {
}
