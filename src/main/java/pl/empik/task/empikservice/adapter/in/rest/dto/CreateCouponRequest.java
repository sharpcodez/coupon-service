package pl.empik.task.empikservice.adapter.in.rest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCouponRequest(
        @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9_-]+") String code,
        @NotNull @Min(1) @Max(1_000_000) Integer maxUsages,
        @NotBlank @Size(min = 2, max = 2) String country) {
}
