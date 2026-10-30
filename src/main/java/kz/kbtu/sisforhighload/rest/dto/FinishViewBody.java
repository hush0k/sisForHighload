package kz.kbtu.sisforhighload.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record FinishViewBody(@NotBlank String viewToken) {
}