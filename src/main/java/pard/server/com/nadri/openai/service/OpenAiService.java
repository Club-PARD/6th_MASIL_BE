package pard.server.com.nadri.openai.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import pard.server.com.nadri.kakaoLocal.dto.Coord;
import pard.server.com.nadri.kakaoLocal.dto.PlaceDto;
import pard.server.com.nadri.openai.dto.*;
import pard.server.com.nadri.plan.dto.req.CreatePlanDto;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Map.entry;

@Service
@Slf4j
public class OpenAiService {

    @Qualifier("openAiWebClient")
    private final WebClient client;
    private final ObjectMapper mapper;

    public OpenAiService(@Qualifier("openAiWebClient") WebClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    /** 메인: 좌표/테마/식사/이동 규칙 포함해서 3개의 Plan 생성 */
    public PlansDto callChatApi(CreatePlanDto req, Coord coord, List<PlaceDto> placeDtos) throws JsonProcessingException {

        // ---------- 0) seed 직렬화 + 조회 맵 ----------
        final String seedJson;
        try {
            seedJson = mapper.writeValueAsString(placeDtos);
        } catch (Exception e) {
            throw new RuntimeException("placeDtos 직렬화 실패", e);
        }

        // ---------- 1) JSON 스키마 ----------
        Map<String, Object> rootSchema = getStringObjectMap();

        // ---------- 2) 프롬프트 ----------
        final String tt = Optional.ofNullable(req.getTimeTable()).map(String::trim).filter(s -> !s.isEmpty()).orElse("09:00~18:00");
        final String transportation = Optional.ofNullable(req.getTransportation()).orElse("대중교통");
        final String theme = Optional.ofNullable(req.getTheme()).orElse("-");
        final String prompt = """
                당신은 여행 일정 기획자입니다. 아래 입력과 seed_places(좌표 포함)만으로 서로 다른 정확히 3개의 Plan을 만드세요.
                각 Plan은 3~10개의 아이템으로 구성되며, 아이템은 세 종류뿐입니다:
                - "이동" (MOVE) : title 은 반드시 "이동"
                MOVE:
                   - 편도: 출발→첫 PLACE 1개, 
                   - 왕복: 출발→첫 PLACE + 마지막 PLACE→출발 2개만.
                   - duration/cost 는 좌표거리 기반(하버사인 + 규칙).
                - 식사 (MEAL)  : title 은 반드시 "아침식사" / "점심식사" / "저녁식사"
                - 장소 (PLACE) : title 은 “{placeName}에서 전시 관람/공연 관람/체험/방문 …” 같은 자연스러운 문장
                
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
                
                [아이템 규칙]
                1) MEAL:
                   - 아침 08:00~10:59, 점심 11:00~15:29, 저녁 17:30~21:59
                   - timeTable과 겹치는 시간대마다 '정확히 한 번' 넣기, duration="60".
                   - 식사 직전/직후 "이동" 금지.
                
                3) PLACE:
                   - seed_places에서만 선택, place_url/placeName/description 필수.
                   - title 은 자연스러운 문장.
                   - **duration 최대 150분**을 넘기지 말 것.
                   - 영화관이라면 duration 120으로 고정, cost는 14000원으로 고정. 
                   - 가능하면 입장료(cost) 기입. (카페/장소는 **최소 비용 보정**)
                   -  ("박물관","미술관","전시")  12000;
                   - ("공연","연극","콘서트","뮤지컬") 30000;
                   - ("체험","공방","키트") 15000;
                   -("테마파크","놀이공원") 35000;
                   - ("전망대","스카이","타워") 10000;
                   - ("카페","디저트") 8000;
                   -("쇼핑","백화점","몰","마켓") 0;
                
                4) 정렬/범위:
                   - 모든 start_time 은 %s 범위 안.
                   - start_time 오름차순, order_num 1부터 연속.
                   - 각 Plan 3~10개.
                
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

        // ---------- 3) 호출 ----------
        Map<String, Object> requestBody = Map.of(
                "model", "gpt-5-nano",
                "instructions", "Return ONLY valid JSON matching the schema. No extra text.",
                "input", prompt,
                "text", Map.of("format", Map.of(
                        "type", "json_schema",
                        "name", "PlansDto",
                        "strict", true,
                        "schema", rootSchema
                ))
        );

        String responseJson = client.post()
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

        return plansDto;
    }

    private static Map<String, Object> getStringObjectMap() {
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
    // 플랜 간 장소 중복 제거

        // (E) PlanDto 구성 + 이동정책 + endTime 세팅

            // 이동 정책 강제(편도=1, 왕복=2)

            // 남는 시간 30분 이하로 보정

            // ✅ 식사 시간대 강제: 시작 시간이 반드시 창 안으로 들어오게

            // order_num 재부여

            // endTime


        // Plan 3개 보정

    // ---------- 플랜 간 장소 중복 제거 ----------





    // ---------- 테마 장소 보장 ----------

    // ---------- 플랜 내 장소 중복 제거 ----------


    // ---------- duration cap ----------

    // ---------- 끝에 장소 하나 추가해서 시간 채우기 ----------


    // ---------- endTime 계산 ----------


    // ===== PLACE 최소 비용 보정 =====


    // -------------------- PLACE 비용 추정(보조) --------------------


    // -------------------- 유틸 --------------------

    // ---------- 이동 정책 강제 ----------


    /**
     * 타임테이블 종료시각(ttEnd) 대비 남는 시간이 30분을 초과하면,
     * 마지막 PLACE(우선) 또는 마지막 비이동 아이템의 duration을 늘려서
     * 남는 시간을 30분 이하로 맞춘다. (이미 끝에 장소 추가 시 남는 자투리만 보정)
     */


    // ✅ 식사 시간대 강제: 시작시간을 식사 창(타임테이블과의 교집합) 안으로 이동, 교집합 없으면 제거


    // 식사 창 상수
}
