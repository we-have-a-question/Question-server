package org.example.questionserver.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class UniversityRequest {
    @NotEmpty(message = "키워드는 최소 1개 이상 입력해야 합니다.")
    private List<String> keywords;

    @Min(value = 1, message = "질문은 최소 1개 이상 생성해야 합니다.")
    @Max(value = 20, message = "질문은 최대 20개까지만 생성 가능합니다.")
    private int questionCount;

    private String major;

}
