package pl.empik.task.empikservice.adapter.in.rest.auth.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}
