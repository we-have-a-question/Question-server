package org.example.questionserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.Getter;
import org.example.questionserver.dto.FeedbackRequest;
import org.example.questionserver.dto.CompanyRequest;
import org.example.questionserver.dto.UniversityRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InterviewService {

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Getter
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public InterviewService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String generateCompanyQuestions(@Valid CompanyRequest request) {
        String prompt = buildCompanyQuestionPrompt(request);
        return callGeminiApi(prompt);
    }

    public String generateUniversityQuestions(@Valid UniversityRequest request) {
        String prompt = buildUniversityQuestionPrompt(request);
        return callGeminiApi(prompt);
    }

    public String evaluateAnswers(FeedbackRequest request) {
        try {
            String qaJson = objectMapper.writeValueAsString(request);
            String prompt = buildEvaluationPrompt(qaJson);
            return callGeminiApi(prompt);
        } catch (Exception e) {
            throw new RuntimeException("JSON 변환 중 오류가 발생했습니다.", e);
        }
    }

    private String buildCompanyQuestionPrompt(CompanyRequest request) {
        String keywordStr = String.join(", ", request.getKeywords());
        String basePrompt;

        basePrompt = """
            당신은 실제 기업의 면접관입니다. 아래 정보를 기반으로 지원자의 역량을 평가할 수 있는 면접 질문을 생성하세요.
            
            
            [입력 정보]
            * 키워드: %s
            * 질문 개수: %d
            * 기업 유형: %s
            * 산업 분야: %s
        
        
            [질문 설계 기준]
            1. 모든 질문은 반드시 입력된 키워드와 직접적으로 연관되어야 합니다.
        
            2. 질문은 다음 3가지 유형을 자연스럽게 혼합하여 구성하세요:
            * 기술 질문 (지식, 개념, 문제 해결 능력)
            * 경험 기반 질문 (실제 경험, 프로젝트, 행동)
            * 상황 질문 (가상의 문제 해결 능력)
        
            3. 모든 질문은 반드시 “열린 질문(Open-ended)” 형태로 작성하세요.
            * 예/아니오로 끝나는 질문 금지
            * 지원자가 자신의 생각과 경험을 설명하도록 유도
        
            4. 질문은 다음 기준을 만족해야 합니다:
            * 구체성: 모호한 표현 없이 명확한 상황 또는 조건 포함
            * 직무 관련성: 실제 업무와 연결되는 질문
            * 사고력 유도: 단순 암기가 아닌 이유, 판단, 과정 설명 요구
        
            5. 반드시 최소 50%% 이상은 “경험 기반 질문”으로 구성하세요.
            * "어떤 경험이 있나요?"가 아니라
            * "어떤 상황에서 무엇을 했고 결과는 어땠는지" 묻는 형태로 작성
        
            6. 질문은 서로 중복되지 않도록 다양하게 구성하세요.
        
            7. 기업 유형과 산업 분야를 반영하여 질문 난이도와 스타일을 조정하세요:
            * 스타트업: 실무 중심, 문제 해결 중심
            * 대기업: 구조적 사고, 협업 경험
            * 공기업: 책임감, 절차 준수, 공공성
            * 산업(domain): 해당 분야 맥락 반영
        
        
            [출력 규칙]
            1. 반드시 JSON 형식으로만 응답하세요.
            2. JSON 외의 텍스트는 절대 포함하지 마세요.
            3. 출력은 반드시 한 줄(minified JSON)로 작성하세요.
            4. 문자열 내부 줄바꿈은 \\n 으로 표현하세요.
        
        
            [출력 JSON 형식]
            {"questions":["질문1","질문2","질문3"]}
        
            """;

            return String.format(basePrompt, keywordStr, request.getQuestionCount(), request.getCompanyType(), request.getDomain());
        }

    private String buildUniversityQuestionPrompt(UniversityRequest request) {
        String keywordStr = String.join(", ", request.getKeywords());
        String basePrompt;

        basePrompt = """
            당신은 대학 입학 면접을 진행하는 면접관입니다. 지원자의 학업 역량, 전공 적합성, 사고력, 학습 의지를 평가하기 위한 질문을 생성하세요.
            
            
            [입력 정보]
            * 키워드: %s
            * 질문 개수: %d
            * 지원 학과: %s
        
        
            [입력 규칙]
            1. 입력 정보는 JSON 형태로 전달되며 추가 설명은 없습니다.
            2. 모든 질문은 반드시 keywords와 major를 기반으로 생성하세요.
        
        
            [질문 설계 기준]
            1. 모든 질문은 반드시 “열린 질문(Open-ended)” 형태로 작성하세요.
               * 예/아니오로 끝나는 질문 금지
               * 지원자가 자신의 생각과 경험을 설명하도록 유도
        
            2. 질문은 키워드를 기반으로 다음 요소를 자연스럽게 포함하도록 구성하세요:
               * 전공 이해 (개념, 원리, 응용)
               * 지원 동기 (관심 계기, 목표)
               * 학업 및 성장 (학습 계획, 발전 방향)
                 ※ 별도의 type 필드를 만들지 말고 질문 내용 안에서 드러나도록 구성
        
            3. 질문은 반드시 사고 과정을 평가할 수 있어야 합니다:
               * “왜 그렇게 생각했는지”
               * “어떤 과정을 통해 판단했는지”
               * “어떻게 적용할 수 있는지”
                 를 포함하도록 설계하세요.
        
            4. 전공 관련 질문 작성 규칙:
               * 단순 정의형 질문 금지
               * 개념 설명 + 실제 적용 또는 사례 연결 요구
        
            5. 경험/학업 질문 작성 규칙:
               * “무엇을 했는지”가 아니라
               * “어떤 상황에서 어떻게 생각하고 행동했는지” 묻는 형태로 작성
        
            6. 질문은 반드시 구체적인 상황 또는 맥락을 포함해야 합니다:
               * 모호한 질문 금지
               * 실제 면접에서 사용할 수 있는 수준으로 작성
        
            7. 질문은 서로 중복되지 않도록 다양하게 구성하세요.
        
            8. 질문 개수는 반드시 {questionCount}와 정확히 일치해야 합니다.
        
        
            [출력 규칙]
            1. 반드시 JSON 형식으로만 응답하세요.
            2. JSON 외의 텍스트는 절대 포함하지 마세요.
            3. 출력은 반드시 한 줄(minified JSON)로 작성하세요. (줄바꿈 금지)
            4. 문자열 내부 줄바꿈은 \\n 으로 표현하세요.
        
        
            [출력 JSON 형식]
            {"questions":["질문1","질문2","질문3"]}
            """;
        return String.format(basePrompt, keywordStr, request.getQuestionCount(), request.getMajor());
    }

    private String buildEvaluationPrompt(String qaJson) {
        return String.format("""
            당신은 면접 평가관입니다. 아래 입력 데이터를 기반으로 지원자의 답변을 평가하세요.
     
     
            [입력 데이터]
            %s
            
            
            [입력 규칙]
            1. qaList는 질문과 답변이 쌍으로 묶인 배열입니다.
            2. 각 question과 answer를 정확히 매칭하여 평가하세요.
            3. 순서를 절대 변경하지 마세요.
            4. 입력 JSON 외의 텍스트는 포함되지 않습니다.


            [평가 지침]
            1. 각 question-answer 쌍에 대해 개선 중심 피드백을 작성하세요.

            2. 모든 답변은 아래 4가지 기준을 기반으로 평가하세요:
               * 논리성: 답변이 서론-본론-결론 구조로 명확하게 구성되어 있는지
               * 구체성: 실제 경험, 행동, 결과 등 구체적인 사례가 포함되어 있는지
               * 직무 적합성: 질문 키워드 및 직무와 관련된 내용이 포함되어 있는지
               * 성장성: 부족한 점을 인식하고 개선하려는 내용이 포함되어 있는지

            3. improvements 필드는 반드시 아래 내용을 모두 포함해야 합니다:
               * 부족한 점 (위 4가지 기준 중 어떤 부분이 부족한지 명확히 지적)
               * 개선 방법 (구체적으로 어떻게 보완해야 하는지)
               * 추천 표현 (실제 면접에서 사용할 수 있는 문장)

            4. improvements 작성 규칙:
               * 반드시 위 4가지 평가 기준을 근거로 설명하세요.
               * 추상적인 표현 금지 (예: "더 구체적으로" 금지)
               * 실제 행동 수준까지 내려가서 작성하세요.
               * 반드시 “면접에서 그대로 사용할 수 있는 문장”을 포함하세요.

            5. modelAnswer 작성 규칙:
               * 서론 → 본론 → 결론 구조를 따르세요.
               * 가능하면 STAR 기법(Situation, Task, Action, Result)을 활용하세요.
               * 반드시 구체적인 행동과 결과를 포함하세요.
               * 실제 면접에서 말할 수 있는 자연스러운 문장으로 작성하세요.

            6. 전체 평가(overallEvaluation)를 반드시 포함하세요:
               * title: 전체 평가를 한 줄로 요약
               * content: 전체적인 강점, 약점, 개선 방향 포함
               * score: 1~100 사이의 정수 (모든 질문에 대한 답변을 종합 평가)


            [출력 규칙]
            1. 반드시 JSON 형식으로만 응답하세요.
            2. JSON 외의 텍스트는 절대 포함하지 마세요.
            3. 출력은 반드시 한 줄(minified JSON)로 작성하세요. (줄바꿈 금지)
            4. JSON의 모든 key와 문자열은 반드시 쌍따옴표(")를 사용하세요.
            5. 문자열 내부 줄바꿈은 반드시 \\n 으로 표현하세요.
            6. 모든 필드는 반드시 채우세요.

            [출력 JSON 형식]
            {"questionFeedbacks":[{"question":"질문 내용","answer":"사용자 답변","improvements":"부족한 점 + 개선 방법 + 추천 표현 포함","modelAnswer":"모범 답안"}, {"question":"질문 내용","answer":"사용자 답변","improvements":"부족한 점 + 개선 방법 + 추천 표현 포함","modelAnswer":"모범 답안"}],"overallEvaluation":{"title":"한 줄 요약 평가","content":"전체 평가","score":1}}
            """, qaJson);
    }

    private String callGeminiApi(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(part));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(content));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        String urlWithKey = apiUrl + "?key=" + apiKey;

        int maxRetry = 3;

        for (int i = 0; i < maxRetry; i++) {
            try {
                String responseJson = restTemplate.postForObject(urlWithKey, entity, String.class);
                return extractTextFromResponse(responseJson);

            } catch (org.springframework.web.client.HttpServerErrorException.ServiceUnavailable e) {
                try {
                    Thread.sleep(1000 * (i + 1)); // 1초 → 2초 → 3초
                } catch (InterruptedException ignored) {}
            }
        }

        // 🔥 fallback (중요)
        return buildFallbackResponse();
    }

    private String buildFallbackResponse() {
        return """
        {"questions":["현재 요청이 많아 질문 생성에 실패했습니다. 잠시 후 다시 시도해주세요."]}
        """;
    }

    private String extractTextFromResponse(String responseJson) {
        try {
            // JsonNode를 사용하여 안전하게 트리 구조로 변환 후 텍스트 추출
            JsonNode rootNode = objectMapper.readTree(responseJson);
            String text = rootNode.at("/candidates/0/content/parts/0/text").asText();

            if (text == null || text.isEmpty()) {
                throw new RuntimeException("Gemini API 응답에서 텍스트를 찾을 수 없습니다.");
            }

            // Markdown 코드 블록(```json 등) 제거 처리
            return text.replace("```json", "").replace("```", "").trim();
        } catch (Exception e) {
            throw new RuntimeException("Gemini API 응답 파싱 실패", e);
        }
    }
}