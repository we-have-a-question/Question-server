package org.example.questionserver.dto;

import lombok.Data;
import java.util.List;

@Data
public class FeedbackRequest {
    private List<QAPair> qaList;

    @Data
    public static class QAPair {
        private String question;
        private String answer;
    }
}
