package pard.server.com.nadri.openai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import pard.server.com.nadri.kakaoLocal.dto.Coord;
import pard.server.com.nadri.openai.dto.PlansDto;
import pard.server.com.nadri.plan.dto.req.CreatePlanDto;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Map.entry;

@Service
@RequiredArgsConstructor
@Slf4j
public class JsonPromptUtil {
    public Map<String, Object> getRootSchema() {
        Map<String, Object> itemSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.ofEntries(
                        // 필수 프로퍼티
                        entry("type", Map.of("type", "string")),
                        entry("title", Map.of("type", "string")),
                        entry("start_time", Map.of("type", "string", "pattern", "^(?:[01]\\d|2[0-3]):[0-5]\\d$")),
                        entry("duration", Map.of("type", "string")),      // 분 단위 문자열
                        entry("order_num", Map.of("type", "integer")),

                        // 선택 프로퍼티
                        entry("cost", Map.of("type", List.of("integer", "null"))),
                        entry("link_url", Map.of("type", List.of("string", "null"))),
                        entry("place_name", Map.of("type", List.of("string", "null"))),
                        entry("description", Map.of("type", List.of("string", "null"),
                                "minLength", 0, "maxLength", 200))
                ),
                "required", List.of("type",
                        "title",
                        "start_time",
                        "duration",
                        "order_num",
                        "cost",
                        "link_url",
                        "place_name",
                        "description")
        );

        Map<String, Object> planSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "order", Map.of("type", "string", "enum", List.of("1", "2", "3")),
                        "planItemDtos", Map.of("type", "array", "minItems", 3, "maxItems", 10, "items", itemSchema)
                ),
                "required", List.of("order", "planItemDtos")
        );

        Map<String, Object> rootSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "planDtos", Map.of("type", "array", "minItems", 3, "maxItems", 3, "items", planSchema)
                ),
                "required", List.of("planDtos")
        );
        return rootSchema;
    }

    public String getPrompt(CreatePlanDto req, Coord coord, String seedJson) {
        // ---------- 2) 프롬프트 ----------
        final String tt = Optional.ofNullable(req.getTimeTable()).map(String::trim).filter(s -> !s.isEmpty()).orElse("09:00~18:00");
        final String transportation = Optional.ofNullable(req.getTransportation()).orElse("대중교통");
        final String theme = Optional.ofNullable(req.getTheme()).orElse("-");
        return """
                당신은 여행 일정 기획자입니다. 아래 입력과 seed_places(좌표 포함)만으로 서로 다른 정확히 3개의 Plan을 만드세요.
                하루동안 여행할 계획 서로 다른 3가지를 만들어주는겁니다.
                각 Plan은 아이템으로 구성되며, 아이템은 세 종류뿐입니다:
                - "이동" (MOVE) : title 은 반드시 "이동"
                MOVE:
                   - 왕복: inOneWay가 false일 때, 마지막 일정은 무조건 이동.
                   - duration은 위치에 따라 5~20분 사이로 결정.
                   - 일정들 사이에 1~2번정도는 MOVE가 들어가줘야 함.
               
               - 식사 (MEAL)  : title 은 반드시 "아침식사" / "점심식사" / "저녁식사"
               MEAL:
                   - 아침 08:00~10:59, 점심 11:00~15:29, 저녁 17:30~21:59
                   - timeTable과 겹치는 시간대마다 '정확히 한 번' 넣기, duration="60".
                   - 식사 직전/직후 "이동" 금지.
                   
                - 장소 (PLACE) : title 은 “{placeName}에서 전시 관람/공연 관람/체험/방문 …” 같은 자연스러운 문장
                PLACE:
                   - TimeTable이 길다면 장소 갯수와 duration을 늘려줘.
                   - seed_places에서만 선택, place_url/placeName/description 필수.
                   - title 은 자연스러운 문장.
                   - duration 최소 60분 이상, 150분 이하. 장소에 따라 넉넉하게 duration 잡아줘도 됨.
                   - 영화관이라면 duration 120으로 고정, cost는 14000원으로 고정. 
                   -  ("박물관","미술관","전시")  12000;
                   - ("공연","연극","콘서트","뮤지컬") 12000;
                   - ("체험","공방","키트") 15000;
                   - ("전망대","스카이","타워") 10000;
                   - ("카페","디저트") 8000;
                   -("쇼핑","백화점","몰","마켓") 0;
                   
               [모든 Item 공통]
                "type", "title", "start_time", "duration", "order_num" 필드는 모든 Item들에게 꼭 있어야 하는 필드.
                "cost", "link_url", "place_name", "description" 필드는 오직 PlaceItem을 위한 필드. MOVE와 MEAL에는 이 필드를 고려할 필요 없음.
                
                - start_time(시작시간) + duration(소요시간)이 일정이 끝난 시간.
                - 반드시 직전 일정이 끝나고, 10~20분 안에 다음 일정을 시작해야 함.
                - 마지막 일정이 끝났을 때 timeTable 끝시간을 절대 넘지 말아야 함. 또, timeTable 끝시간을 30분 이상 남기지 않아야 함.
                - 3개의 PlanDto 안에 PlanItemDto들중 PlaceItem은 최대한 겹치지 않게 해줘. (새로운 장소들로 PlanDto가 만들어지도록 해줘)
                - 모든 cost를 합쳤을 때 budget_per_person을 넘지 않도록 해줘.
                
                [입력 파라미터]
                - origin: %s
                - budget_per_person: %s 원
                - headcount: %s 명
                - transportation: %s
                - date: %s
                - timeTable: %s
                - theme: %s
                - origin_coord(lat,lon): %s, %s
                - isOneWay: %s
                
                4) 정렬/범위:
                   - 모든 start_time 은 %s 범위 안.
                   - start_time 오름차순, order_num 1부터 연속.
                   - 각 PlanItem 3~10개.
                
                5) 예산/인원:
                   - 총예산 = budget_per_person × headcount 내.
                
                [출력 형식]
                - 오직 JSON. 필드: title, start_time, duration, order_num, cost, place_url, placeName, description
                - planDtos 는 정확히 3개, order 는 "1","2","3".
                
                [seed_places JSON]
                %s
                """.formatted(
                Optional.ofNullable(req.getOrigin()).orElse("-"),
                String.valueOf(req.getBudget()),
                String.valueOf(req.getHeadcount()),
                transportation,
                Optional.ofNullable(req.getDate()).orElse("-"),
                tt,
                theme,
                coord == null ? "0.0" : String.valueOf(coord.x()),
                coord == null ? "0.0" : String.valueOf(coord.y()),
                String.valueOf(req.isOneWay()),
                tt,
                seedJson
        );
    }

    public String getResponseJson(String prompt, Map<String, Object> rootSchema, WebClient client){
        Map<String, Object> requestBody = Map.of(
                "model", "gpt-5",
                "instructions", "Return ONLY valid JSON matching the schema. No extra text.",
                "input", prompt,
                "text", Map.of("format", Map.of(
                        "type", "json_schema",
                        "name", "PlansDto",
                        "strict", true,
                        "schema", rootSchema
                ))
        );

        return client.post()
                .uri("/v1/responses")
                .bodyValue(requestBody)
                .exchangeToMono(res -> {
                    if (res.statusCode().is2xxSuccessful()) return res.bodyToMono(String.class);
                    return res.bodyToMono(String.class)
                            .defaultIfEmpty("(empty)")
                            .flatMap(body -> {
                                log.error("OpenAI error {} body: {}", res.statusCode(), body);
                                return Mono.error(new RuntimeException("OpenAI " + res.statusCode() + " - " + body));
                            });
                })
                .block();
    }

    // rootSchema -> PlansDto로 매핑, 로그찍기.
    public PlansDto getPlansDto(String responseJson, ObjectMapper mapper) throws JsonProcessingException {

        JsonNode root = mapper.readTree(responseJson);
        log.info("OpenAI response -> root: {}", root);

        JsonNode output = root.path("output");

        JsonNode finalOutput = "message".equals(output.get(0).path("type").asText()) && !output.isEmpty()
                ? output.get(0)
                : output.get(1);

        String text = finalOutput.path("content").get(0).path("text").asText();

        String pretty = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(mapper.readTree(text));

        log.info("실제 모델 출력(JSON):\n{}", pretty);

        PlansDto plansDto = mapper.readValue(text, PlansDto.class);

        log.info(plansDto.toString());

        return plansDto;
    }
}
