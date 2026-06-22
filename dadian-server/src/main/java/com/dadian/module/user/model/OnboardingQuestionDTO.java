package com.dadian.module.user.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingQuestionDTO {
    private String questionText;
    private List<String> options;
}
