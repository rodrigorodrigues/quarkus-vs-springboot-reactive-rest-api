package com.github.springboot.dto;

import java.time.Instant;

import javax.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompanyDto {
    private String id;
    @NotBlank
    private String name;
    private String createdByUser;
    private Boolean activated = true;
    private Instant createdDate;
    private String lastModifiedByUser;
    private Instant lastModifiedDate;
}
