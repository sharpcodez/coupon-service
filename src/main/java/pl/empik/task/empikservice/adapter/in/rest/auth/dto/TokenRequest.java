package pl.empik.task.empikservice.adapter.in.rest.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRequest(@NotBlank String username, @NotBlank String password) {}
