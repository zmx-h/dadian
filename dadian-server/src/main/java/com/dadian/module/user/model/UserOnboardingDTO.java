package com.dadian.module.user.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserOnboardingDTO {

    @NotNull
    @Min(1)
    @Max(3)
    private Integer socialTrait;

    @NotNull
    @Min(1)
    @Max(3)
    private Integer weekendStyle;

    @NotNull
    @Min(1)
    @Max(3)
    private Integer crowdFeeling;
}
