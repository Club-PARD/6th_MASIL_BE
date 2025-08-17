package pard.server.com.nadri.openai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class OpenAiService {
    @Qualifier("openAiWebClient")
    private final WebClient client;

    public OpenAiService(@Qualifier("openAiWebClient") WebClient client){
        this.client = client;
    }

    public void callChatApi() {
//        // 1) JSON Schema 정의
//        Map<String, Object> responseSchema = Map.of(
//                "name", "ResponseChatDto",
//                "strict", true,
//                "schema", Map.of(
//                        "type", "object",
//                        "additionalProperties", false,
//                        "properties", Map.of(
//                                "placeName", Map.of("type", "string"),
//                                "placeLocation", Map.of("type", "string"),
//                                "timetable", Map.of("type", "string"),
//                                "time", Map.of("type", "integer")
//                        ),
//                        "required", List.of("placeName", "placeLocation", "timetable", "time")
//                )
//        );
//
//        // 2) 프롬프트 작성
//        String userPrompt = String.format(
//                "출발지는 %s이고, 도착지 식당의 이름(placeName)은 유일한 식탁이고, 주소(placeLocation)는 부산 남구 용소로13번길 62로 고정해줘, " +
//                        "오늘 영업시간(timetable, 예: '09:00-21:00')을 알려줘. 그리고 도보로 출발지로부터 식당까지 걸리는 소요시간(time)을 알려줘 " +
//                        "반드시 JSON 스키마에 맞게만 응답하고, 다른 설명은 절대 하지 마.",
//                createPlanDto.getOrigin(), createPlanDto.getBudget()
//        );
//
//        // 3) 요청 바디 (Responses API)
//        Map<String, Object> requestBody = Map.of(
//                "model", "gpt-4o-mini",
//                "instructions", "You are a helpful assistant. Return ONLY valid JSON matching the schema. No extra text.",
//                "input", userPrompt,
//                "text", Map.of(
//                        "format", Map.of(
//                                "type", "json_schema",
//                                "name", "ResponseChatDto",
//                                "strict", true,
//                                "schema", Map.of(
//                                        "type", "object",
//                                        "additionalProperties", false,
//                                        "properties", Map.of(
//                                                "placeName", Map.of("type", "string"),
//                                                "placeLocation", Map.of("type", "string"),
//                                                "timetable", Map.of("type", "string"),
//                                                "time", Map.of("type", "integer")
//                                        ),
//                                        "required", List.of("placeName", "placeLocation", "timetable", "time")
//                                )
//                        )
//                ),
//                "temperature", 0
//        );
//
//        // 4) 호출 및 응답 로깅
//        String responseJson = client.post()
//                .uri("/v1/responses")
//                .bodyValue(requestBody)
//                .exchangeToMono(res -> {
//                    if (res.statusCode().is2xxSuccessful()) {
//                        return res.bodyToMono(String.class);
//                    } else {
//                        return res.bodyToMono(String.class)
//                                .defaultIfEmpty("(empty)")
//                                .flatMap(body -> {
//                                    log.error("OpenAI error {} body: {}", res.statusCode(), body);
//                                    return Mono.error(new RuntimeException("OpenAI " + res.statusCode() + " - " + body));
//                                });
//                    }
//                })
//                .block();
//
//        // 디버깅을 위한 전체 응답 로깅
//        log.info("OpenAI 전체 응답: {}", responseJson);
//
//        // 5) 응답 JSON → DTO 변환
//        try {
//            JsonNode root = mapper.readTree(responseJson);
//            log.info("파싱된 JSON 루트: {}", root.toString());
//
//            // /v1/responses API의 응답 구조 확인
//            if (root.has("output")) {
//                JsonNode output = root.path("output");
//                log.info("output 노드: {}", output.toString());
//
//                if (output.isArray() && output.size() > 0) {
//                    JsonNode firstOutput = output.get(0);
//                    log.info("첫 번째 output: {}", firstOutput.toString());
//
//                    // content 필드에서 JSON 추출
//                    if (firstOutput.has("content")) {
//                        JsonNode content = firstOutput.path("content");
//                        log.info("content 노드: {}", content.toString());
//
//                        String jsonContent = null;
//                        if (content.isTextual()) {
//                            jsonContent = content.asText();
//                        } else if (content.isArray() && content.size() > 0) {
//                            JsonNode firstContent = content.get(0);
//                            if (firstContent.has("text")) {
//                                jsonContent = firstContent.path("text").asText();
//                            }
//                        }
//
//                        if (jsonContent != null && !jsonContent.isEmpty()) {
//                            log.info("추출된 JSON 내용: {}", jsonContent);
//                            ResponseChatDto responseChatDto = mapper.readValue(jsonContent, ResponseChatDto.class);
//                            placeRepo.save(Place.from(responseChatDto));
//                            log.info("식당 정보 저장 완료: {}", responseChatDto.getPlaceName());
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            log.error("응답 파싱 실패", e);
//            throw new RuntimeException("응답 파싱 실패: " + e.getMessage(), e);
//
//            }
    }
}

