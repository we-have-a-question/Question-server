package org.example.questionserver;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.questionserver.dto.FeedbackRequest;
import org.example.questionserver.dto.CompanyRequest;
import org.example.questionserver.dto.UniversityRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interview")
public class InterviewController {

    private final InterviewService interviewService;

    // 1. 면접 질문 생성 API
    @PostMapping("/questions/company")
    public ResponseEntity<String> generateCompanyQuestions(@Valid @RequestBody CompanyRequest request) {
        String jsonResponse = interviewService.generateCompanyQuestions(request);
        return ResponseEntity.ok().header("Content-Type", "application/json").body(jsonResponse);
    }

    @PostMapping("/questions/university")
    public ResponseEntity<String> generateUniversityQuestions(@Valid @RequestBody UniversityRequest request) {
        String jsonResponse = interviewService.generateUniversityQuestions(request);
        return ResponseEntity.ok().header("Content-Type", "application/json").body(jsonResponse);
    }

    // 2. 답변 제출 및 피드백 생성 API
    @PostMapping("/feedback")
    public ResponseEntity<String> getFeedback(@RequestBody FeedbackRequest request) {
        String jsonResponse = interviewService.evaluateAnswers(request);
        return ResponseEntity.ok().header("Content-Type", "application/json").body(jsonResponse);
    }
}
