package pard.server.com.nadri.openai.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    public PlansDto callChatApi(CreatePlanDto req, Coord coord, List<PlaceDto> placeDtos) {

        // ---------- 0) seed 직렬화 + 조회 맵 ----------
        final String seedJson;
        try {
            seedJson = mapper.writeValueAsString(placeDtos);
        } catch (Exception e) {
            throw new RuntimeException("placeDtos 직렬화 실패", e);
        }

        final Map<String, PlaceDto> seedByUrl = placeDtos.stream()
                .filter(p -> p.getPlaceUrl() != null)
                .collect(Collectors.toMap(PlaceDto::getPlaceUrl, Function.identity(), (a, b) -> a));

        final List<String> allowedUrls = placeDtos.stream()
                .map(PlaceDto::getPlaceUrl)
                .filter(Objects::nonNull)
                .toList();

        // ---------- 1) JSON 스키마 ----------
        Map<String, Object> itemSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.ofEntries(
                        entry("title",       Map.of("type", "string")),
                        entry("start_time",  Map.of("type", "string", "pattern", "^(?:[01]\\d|2[0-3]):[0-5]\\d$")),
                        entry("duration",    Map.of("type", "string")),      // 분 단위 문자열
                        entry("order_num",   Map.of("type", "integer")),
                        entry("cost",        Map.of("type", "integer")),
                        entry("place_url",   allowedUrls.isEmpty()
                                ? Map.of("type", List.of("string","null"))
                                : Map.of("type", List.of("string","null"), "enum", allowedUrls)),
                        entry("placeName",   Map.of("type", List.of("string","null"))),
                        entry("description", Map.of("type", List.of("string","null"), "minLength", 0, "maxLength", 200))
                ),
                "required", List.of("title","start_time","duration","order_num","cost","place_url","placeName","description")
        );

        Map<String, Object> planSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "order", Map.of("type","string","enum",List.of("1","2","3")),
                        "planItemDtos", Map.of("type","array","minItems",3,"maxItems",10,"items", itemSchema)
                ),
                "required", List.of("order","planItemDtos")
        );

        Map<String, Object> rootSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "planDtos", Map.of("type", "array","minItems", 3,"maxItems", 3,"items", planSchema)
                ),
                "required", List.of("planDtos")
        );

        // ---------- 2) 프롬프트 ----------
        final String tt = Optional.ofNullable(req.getTimeTable()).map(String::trim).filter(s -> !s.isEmpty()).orElse("09:00~18:00");
        final String transportation = Optional.ofNullable(req.getTransportation()).orElse("대중교통");
        final String theme = Optional.ofNullable(req.getTheme()).orElse("-");
        final String prompt = """
당신은 여행 일정 기획자입니다. 아래 입력과 seed_places(좌표 포함)만으로 서로 다른 정확히 3개의 Plan을 만드세요.
각 Plan은 3~10개의 아이템으로 구성되며, 아이템은 세 종류뿐입니다:
- "이동" (MOVE) : title 은 반드시 "이동"
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

2) MOVE:
   - title="이동".
   - 편도: 출발→첫 PLACE 1개, 왕복: 출발→첫 PLACE + 마지막 PLACE→출발 2개만.
   - duration/cost 는 좌표거리 기반(하버사인 + 규칙).

3) PLACE:
   - seed_places에서만 선택, place_url/placeName/description 필수.
   - title 은 자연스러운 문장.
   - **duration 최대 150분**을 넘기지 말 것.
   - 가능하면 입장료(cost) 기입. (카페/장소는 **최소 비용 보정**)

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
                "model", "gpt-4o-mini",
                "instructions", "Return ONLY valid JSON matching the schema. No extra text.",
                "input", prompt,
                "text", Map.of("format", Map.of(
                        "type", "json_schema",
                        "name", "PlansDto",
                        "strict",  false,
                        "schema", rootSchema
                )),
                "temperature", 0
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

        log.info("OpenAI 전체 응답(raw): {}", responseJson);

        // ---------- 4) 파싱 ----------
        GenRoot parsed;
        try {
            JsonNode root = mapper.readTree(responseJson);
            String jsonContent = extractPayload(root);
            if (jsonContent == null || jsonContent.isBlank())
                throw new IllegalStateException("모델 응답에서 JSON 본문을 찾지 못했습니다.");
            parsed = mapper.readValue(jsonContent, GenRoot.class);
        } catch (Exception e) {
            log.error("응답 파싱 실패", e);
            throw new RuntimeException("응답 파싱 실패: " + e.getMessage(), e);
        }

        // ---------- 5) 후처리 ----------
        final String themeKw = Optional.ofNullable(req.getTheme()).orElse("").trim();
        final LocalTime ttStart = parseTime(tt.split("~")[0]);
        final LocalTime ttEnd   = parseTime(tt.split("~")[1]);

        List<List<PlanItemDto>> planItemsList = new ArrayList<>();
        List<String> originalOrders = new ArrayList<>();

        if (parsed.getPlanDtos() != null) {
            for (GenPlan gp : parsed.getPlanDtos()) {
                List<GenItem> genItems = Optional.ofNullable(gp.getPlanItemDtos()).orElseGet(ArrayList::new);
                if (genItems.size() > 10) genItems = genItems.subList(0, 10);

                // 시작시간 정렬
                genItems.sort(Comparator.comparing(it -> parseTime(it.getStartTime())));

                // 모델 출력 → 내부 DTO 매핑
                List<PlanItemDto> mapped = new ArrayList<>();
                for (GenItem it : genItems) {
                    LocalTime st = parseTime(it.getStartTime());
                    String title = Optional.ofNullable(it.getTitle()).orElse("").trim();
                    String duration = Optional.ofNullable(it.getDuration()).orElse("60");
                    Integer cost = it.getCost();

                    boolean isMove = "이동".equals(title);
                    boolean isMeal = title.contains("식사");
                    boolean hasPlace = it.getPlaceUrl() != null || it.getPlaceName() != null;

                    if (isMove) {
                        mapped.add(MoveItemDto.of("이동", 0, cost == null ? 0 : cost, duration, st));
                    } else if (isMeal) {
                        mapped.add(MealItemDto.of(title, 0, "60", st));
                    } else if (hasPlace) {
                        String desc = Optional.ofNullable(it.getDescription()).orElse("");
                        Integer entry = cost != null ? cost : estimatePlaceCost(null, it.getPlaceName(), desc);
                        entry = enforcePlaceMinCost(it.getPlaceName(), desc, entry); // 최소 비용 보정
                        int d = Math.min(parseMinutes(duration), 150);               // PLACE duration cap

                        PlaceItemDto dto = PlaceItemDto.of(
                                title.isEmpty() && it.getPlaceName()!=null ? it.getPlaceName()+" 방문" : title,
                                0,
                                entry,
                                String.valueOf(Math.max(0, d)),
                                st,
                                desc,
                                it.getPlaceUrl(),
                                it.getPlaceName()
                        );
                        try { dto.getClass().getMethod("setPlaceName", String.class).invoke(dto, it.getPlaceName()); } catch (Exception ignore) {}
                        mapped.add(dto);
                    }
                }

                // 테마 보장 & 중복 제거 & 정렬
                ensureThemePlace(mapped, themeKw, placeDtos, seedByUrl);
                mapped = dedupePlacesInPlan(mapped);
                mapped.sort(Comparator.comparing(PlanItemDto::getStartTime, Comparator.nullsFirst(Comparator.naturalOrder())));

                // duration cap 재확인
                capPlaceDurations(mapped, 150);

                // 끝에 큰 여유가 있으면 장소 추가
                appendExtraPlaceIfEndGapBig(mapped, ttEnd, placeDtos, themeKw);

                planItemsList.add(mapped);
                originalOrders.add(gp.getOrder());
            }
        }

        // 플랜 간 장소 중복 제거
        dedupePlacesAcrossPlans(planItemsList, themeKw, placeDtos);

        // (E) PlanDto 구성 + 이동정책 + endTime 세팅
        List<PlanDto> out = new ArrayList<>();
        for (int i = 0; i < planItemsList.size(); i++) {
            List<PlanItemDto> items = planItemsList.get(i);

            // 이동 정책 강제(편도=1, 왕복=2)
            enforceMovePolicy(items, req.isOneWay(), ttStart, ttEnd);

            // 남는 시간 30분 이하로 보정
            stretchPlanToTimeTable(items, ttEnd);

            // ✅ 식사 시간대 강제: 시작 시간이 반드시 창 안으로 들어오게
            enforceMealWindows(items, ttStart, ttEnd);

            // order_num 재부여
            int ord = 1; for (PlanItemDto x : items) x.setOrderNum(ord++);

            // endTime
            String endTime = computePlanEndTime(items);

            PlanDto plan = new PlanDto(i < originalOrders.size() ? originalOrders.get(i) : String.valueOf(i+1), items);
            try {
                Method m = plan.getClass().getMethod("setEndTime", String.class);
                m.invoke(plan, endTime);
            } catch (Exception ignore) { log.debug("PlanDto#setEndTime 설정 실패(무시): {}", ignore.toString()); }

            out.add(plan);
        }

        // Plan 3개 보정
        if (out.size() > 3) out = out.subList(0, 3);
        while (out.size() < 3) {
            PlanDto empty = new PlanDto(String.valueOf(out.size()+1), new ArrayList<>());
            try { empty.getClass().getMethod("setEndTime", String.class).invoke(empty, (String) null); } catch (Exception ignore) {}
            out.add(empty);
        }
        for (int i = 0; i < out.size(); i++) out.get(i).setOrder(String.valueOf(i+1));

        PlansDto result = new PlansDto();
        result.setPlanDtos(out);

        try {
            log.info("plansDto 전체 =>\n{}", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
            if (result.getPlanDtos() != null) {
                for (int i = 0; i < result.getPlanDtos().size(); i++) {
                    log.info("planDto[{}] =>\n{}", i,
                            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result.getPlanDtos().get(i)));
                }
            }
        } catch (Exception e) {
            log.warn("plansDto 로그 직렬화 실패", e);
        }

        return result;
    }

//    public PlansDto reloadPlans(CreatePlanDto req, Coord coord, List<PlaceDto> placeDtos, Long plansId){
//
//        // ---------- 0) seed 직렬화 + 조회 맵 ----------
//        final String seedJson;
//        try {
//            seedJson = mapper.writeValueAsString(placeDtos);
//        } catch (Exception e) {
//            throw new RuntimeException("placeDtos 직렬화 실패", e);
//        }
//
//        final Map<String, PlaceDto> seedByUrl = placeDtos.stream()
//                .filter(p -> p.getPlaceUrl() != null)
//                .collect(Collectors.toMap(PlaceDto::getPlaceUrl, Function.identity(), (a, b) -> a));
//
//        final List<String> allowedUrls = placeDtos.stream()
//                .map(PlaceDto::getPlaceUrl)
//                .filter(Objects::nonNull)
//                .toList();
//
//        // ---------- 1) JSON 스키마 ----------
//        Map<String, Object> itemSchema = Map.of(
//                "type", "object",
//                "additionalProperties", false,
//                "properties", Map.ofEntries(
//                        entry("title",       Map.of("type", "string")),
//                        entry("start_time",  Map.of("type", "string", "pattern", "^(?:[01]\\d|2[0-3]):[0-5]\\d$")),
//                        entry("duration",    Map.of("type", "string")),      // 분 단위 문자열
//                        entry("order_num",   Map.of("type", "integer")),
//                        entry("cost",        Map.of("type", "integer")),
//                        entry("place_url",   allowedUrls.isEmpty()
//                                ? Map.of("type", List.of("string","null"))
//                                : Map.of("type", List.of("string","null"), "enum", allowedUrls)),
//                        entry("placeName",   Map.of("type", List.of("string","null"))),
//                        entry("description", Map.of("type", List.of("string","null"), "minLength", 0, "maxLength", 200))
//                ),
//                "required", List.of("title","start_time","duration","order_num","cost","place_url","placeName","description")
//        );
//
//        Map<String, Object> planSchema = Map.of(
//                "type", "object",
//                "additionalProperties", false,
//                "properties", Map.of(
//                        "order", Map.of("type","string","enum",List.of("1","2","3")),
//                        "planItemDtos", Map.of("type","array","minItems",3,"maxItems",10,"items", itemSchema)
//                ),
//                "required", List.of("order","planItemDtos")
//        );
//
//        Map<String, Object> rootSchema = Map.of(
//                "type", "object",
//                "additionalProperties", false,
//                "properties", Map.of(
//                        "planDtos", Map.of("type", "array","minItems", 3,"maxItems", 3,"items", planSchema)
//                ),
//                "required", List.of("planDtos")
//        );
//
//        // ---------- 2) 프롬프트 ----------
//        final String tt = Optional.ofNullable(req.getTimeTable()).map(String::trim).filter(s -> !s.isEmpty()).orElse("09:00~18:00");
//        final String transportation = Optional.ofNullable(req.getTransportation()).orElse("대중교통");
//        final String theme = Optional.ofNullable(req.getTheme()).orElse("-");
//        final String prompt = """
//당신은 여행 일정 기획자입니다. 아래 입력과 seed_places(좌표 포함)만으로 서로 다른 정확히 3개의 Plan을 만드세요.
//각 Plan은 3~10개의 아이템으로 구성되며, 아이템은 세 종류뿐입니다:
//- "이동" (MOVE) : title 은 반드시 "이동"
//- 식사 (MEAL)  : title 은 반드시 "아침식사" / "점심식사" / "저녁식사"
//- 장소 (PLACE) : title 은 “{placeName}에서 전시 관람/공연 관람/체험/방문 …” 같은 자연스러운 문장
//
//[입력 파라미터]
//- origin: %s
//- budget_per_person: %s 원
//- headcount: %s 명
//- transportation: %s
//- date: %s
//- timeTable: %s
//- theme: %s
//- origin_coord(lat,lon): %s, %s
//- isOneWay: %s
//
//[아이템 규칙]
//1) MEAL:
//   - 아침 08:00~10:59, 점심 11:00~15:29, 저녁 17:30~21:59
//   - timeTable과 겹치는 시간대마다 '정확히 한 번' 넣기, duration="60".
//   - 식사 직전/직후 "이동" 금지.
//
//2) MOVE:
//   - title="이동".
//   - 편도: 출발→첫 PLACE 1개, 왕복: 출발→첫 PLACE + 마지막 PLACE→출발 2개만.
//   - duration/cost 는 좌표거리 기반(하버사인 + 규칙).
//
//3) PLACE:
//   - seed_places에서만 선택, place_url/placeName/description 필수.
//   - title 은 자연스러운 문장.
//   - **duration 최대 150분**을 넘기지 말 것.
//   - 가능하면 입장료(cost) 기입. (카페/장소는 **최소 비용 보정**)
//
//4) 정렬/범위:
//   - 모든 start_time 은 %s 범위 안.
//   - start_time 오름차순, order_num 1부터 연속.
//   - 각 Plan 3~10개.
//
//5) 예산/인원:
//   - 총예산 = budget_per_person × headcount 내.
//
//[출력 형식]
//- 오직 JSON. 필드: title, start_time, duration, order_num, cost, place_url, placeName, description
//- planDtos 는 정확히 3개, order 는 "1","2","3".
//
//[seed_places JSON]
//%s
//""".formatted(
//                Optional.ofNullable(req.getOrigin()).orElse("-"),
//                String.valueOf(req.getBudget()),
//                String.valueOf(req.getHeadcount()),
//                transportation,
//                Optional.ofNullable(req.getDate()).orElse("-"),
//                tt,
//                theme,
//                coord == null ? "0.0" : String.valueOf(coord.x()),
//                coord == null ? "0.0" : String.valueOf(coord.y()),
//                String.valueOf(req.isOneWay()),
//                tt,
//                seedJson
//        );
//
//        // ---------- 3) 호출 ----------
//        Map<String, Object> requestBody = Map.of(
//                "model", "gpt-4o-mini",
//                "instructions", "Return ONLY valid JSON matching the schema. No extra text.",
//                "input", prompt,
//                "text", Map.of("format", Map.of(
//                        "type", "json_schema",
//                        "name", "PlansDto",
//                        "strict",  false,
//                        "schema", rootSchema
//                )),
//                "temperature", 0
//        );
//
//        String responseJson = client.post()
//                .uri("/v1/responses")
//                .bodyValue(requestBody)
//                .exchangeToMono(res -> {
//                    if (res.statusCode().is2xxSuccessful()) return res.bodyToMono(String.class);
//                    return res.bodyToMono(String.class)
//                            .defaultIfEmpty("(empty)")
//                            .flatMap(body -> {
//                                log.error("OpenAI error {} body: {}", res.statusCode(), body);
//                                return Mono.error(new RuntimeException("OpenAI " + res.statusCode() + " - " + body));
//                            });
//                })
//                .block();
//
//        log.info("OpenAI 전체 응답(raw): {}", responseJson);
//
//        // ---------- 4) 파싱 ----------
//        GenRoot parsed;
//        try {
//            JsonNode root = mapper.readTree(responseJson);
//            String jsonContent = extractPayload(root);
//            if (jsonContent == null || jsonContent.isBlank())
//                throw new IllegalStateException("모델 응답에서 JSON 본문을 찾지 못했습니다.");
//            parsed = mapper.readValue(jsonContent, GenRoot.class);
//        } catch (Exception e) {
//            log.error("응답 파싱 실패", e);
//            throw new RuntimeException("응답 파싱 실패: " + e.getMessage(), e);
//        }
//
//        // ---------- 5) 후처리 ----------
//        final String themeKw = Optional.ofNullable(req.getTheme()).orElse("").trim();
//        final LocalTime ttStart = parseTime(tt.split("~")[0]);
//        final LocalTime ttEnd   = parseTime(tt.split("~")[1]);
//
//        List<List<PlanItemDto>> planItemsList = new ArrayList<>();
//        List<String> originalOrders = new ArrayList<>();
//
//        if (parsed.getPlanDtos() != null) {
//            for (GenPlan gp : parsed.getPlanDtos()) {
//                List<GenItem> genItems = Optional.ofNullable(gp.getPlanItemDtos()).orElseGet(ArrayList::new);
//                if (genItems.size() > 10) genItems = genItems.subList(0, 10);
//
//                // 시작시간 정렬
//                genItems.sort(Comparator.comparing(it -> parseTime(it.getStartTime())));
//
//                // 모델 출력 → 내부 DTO 매핑
//                List<PlanItemDto> mapped = new ArrayList<>();
//                for (GenItem it : genItems) {
//                    LocalTime st = parseTime(it.getStartTime());
//                    String title = Optional.ofNullable(it.getTitle()).orElse("").trim();
//                    String duration = Optional.ofNullable(it.getDuration()).orElse("60");
//                    Integer cost = it.getCost();
//
//                    boolean isMove = "이동".equals(title);
//                    boolean isMeal = title.contains("식사");
//                    boolean hasPlace = it.getPlaceUrl() != null || it.getPlaceName() != null;
//
//                    if (isMove) {
//                        mapped.add(MoveItemDto.of("이동", 0, cost == null ? 0 : cost, duration, st));
//                    } else if (isMeal) {
//                        mapped.add(MealItemDto.of(title, 0, "60", st));
//                    } else if (hasPlace) {
//                        String desc = Optional.ofNullable(it.getDescription()).orElse("");
//                        Integer entry = cost != null ? cost : estimatePlaceCost(null, it.getPlaceName(), desc);
//                        entry = enforcePlaceMinCost(it.getPlaceName(), desc, entry); // 최소 비용 보정
//                        int d = Math.min(parseMinutes(duration), 150);               // PLACE duration cap
//
//                        PlaceItemDto dto = PlaceItemDto.of(
//                                title.isEmpty() && it.getPlaceName()!=null ? it.getPlaceName()+" 방문" : title,
//                                0,
//                                entry,
//                                String.valueOf(Math.max(0, d)),
//                                st,
//                                desc,
//                                it.getPlaceUrl(),
//                                it.getPlaceName()
//                        );
//                        try { dto.getClass().getMethod("setPlaceName", String.class).invoke(dto, it.getPlaceName()); } catch (Exception ignore) {}
//                        mapped.add(dto);
//                    }
//                }
//
//                // 테마 보장 & 중복 제거 & 정렬
//                ensureThemePlace(mapped, themeKw, placeDtos, seedByUrl);
//                mapped = dedupePlacesInPlan(mapped);
//                mapped.sort(Comparator.comparing(PlanItemDto::getStartTime, Comparator.nullsFirst(Comparator.naturalOrder())));
//
//                // duration cap 재확인
//                capPlaceDurations(mapped, 150);
//
//                // 끝에 큰 여유가 있으면 장소 추가
//                appendExtraPlaceIfEndGapBig(mapped, ttEnd, placeDtos, themeKw);
//
//                planItemsList.add(mapped);
//                originalOrders.add(gp.getOrder());
//            }
//        }
//
//        // 플랜 간 장소 중복 제거
//        dedupePlacesAcrossPlans(planItemsList, themeKw, placeDtos);
//
//        // (E) PlanDto 구성 + 이동정책 + endTime 세팅
//        List<PlanDto> out = new ArrayList<>();
//        for (int i = 0; i < planItemsList.size(); i++) {
//            List<PlanItemDto> items = planItemsList.get(i);
//
//            // 이동 정책 강제(편도=1, 왕복=2)
//            enforceMovePolicy(items, req.isOneWay(), ttStart, ttEnd);
//
//            // 남는 시간 30분 이하로 보정
//            stretchPlanToTimeTable(items, ttEnd);
//
//            // ✅ 식사 시간대 강제: 시작 시간이 반드시 창 안으로 들어오게
//            enforceMealWindows(items, ttStart, ttEnd);
//
//            // order_num 재부여
//            int ord = 1; for (PlanItemDto x : items) x.setOrderNum(ord++);
//
//            // endTime
//            String endTime = computePlanEndTime(items);
//
//            PlanDto plan = new PlanDto(i < originalOrders.size() ? originalOrders.get(i) : String.valueOf(i+1), items);
//            try {
//                Method m = plan.getClass().getMethod("setEndTime", String.class);
//                m.invoke(plan, endTime);
//            } catch (Exception ignore) { log.debug("PlanDto#setEndTime 설정 실패(무시): {}", ignore.toString()); }
//
//            out.add(plan);
//        }
//
//        // Plan 3개 보정
//        if (out.size() > 3) out = out.subList(0, 3);
//        while (out.size() < 3) {
//            PlanDto empty = new PlanDto(String.valueOf(out.size()+1), new ArrayList<>());
//            try { empty.getClass().getMethod("setEndTime", String.class).invoke(empty, (String) null); } catch (Exception ignore) {}
//            out.add(empty);
//        }
//        for (int i = 0; i < out.size(); i++) out.get(i).setOrder(String.valueOf(i+1));
//
//        PlansDto result = new PlansDto();
//        result.setPlanDtos(out);
//
//        try {
//            log.info("plansDto 전체 =>\n{}", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
//            if (result.getPlanDtos() != null) {
//                for (int i = 0; i < result.getPlanDtos().size(); i++) {
//                    log.info("planDto[{}] =>\n{}", i,
//                            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result.getPlanDtos().get(i)));
//                }
//            }
//        } catch (Exception e) {
//            log.warn("plansDto 로그 직렬화 실패", e);
//        }
//
//        return result;
//    }

    // ---------- 플랜 간 장소 중복 제거 ----------
    private void dedupePlacesAcrossPlans(List<List<PlanItemDto>> plans,
                                         String themeKw,
                                         List<PlaceDto> seedList) {
        Set<String> globalUsed = new HashSet<>();
        for (int pi = 0; pi < plans.size(); pi++) {
            List<PlanItemDto> items = plans.get(pi);

            Set<String> planKeys = items.stream()
                    .filter(PlaceItemDto.class::isInstance)
                    .map(PlaceItemDto.class::cast)
                    .map(this::placeKey)
                    .filter(k -> !k.isBlank())
                    .collect(Collectors.toSet());

            int themeCount = (int) items.stream()
                    .filter(PlaceItemDto.class::isInstance)
                    .map(PlaceItemDto.class::cast)
                    .filter(p -> matchesTheme(p, themeKw))
                    .count();

            for (int idx = 0; idx < items.size(); idx++) {
                if (!(items.get(idx) instanceof PlaceItemDto p)) continue;
                String key = placeKey(p);
                if (key.isBlank()) continue;

                if (globalUsed.add(key)) continue;

                boolean isThemeItem = matchesTheme(p, themeKw);
                boolean needThemeReplacement = isThemeItem && themeCount <= 1;

                PlaceDto cand = pickReplacementCandidate(seedList, globalUsed, planKeys, needThemeReplacement, themeKw);
                if (cand == null) continue;

                PlaceItemDto np = createPlaceFromSeed(cand, p.getStartTime(), p.getDuration(),
                        needThemeReplacement ? (safeStr(cand.getPlaceName()) + " 방문(테마)") : (safeStr(cand.getPlaceName()) + " 방문"),
                        needThemeReplacement ? "테마 관련 장소 방문" : Optional.ofNullable(p.getDescription()).orElse("대체 장소 방문"));

                items.set(idx, np);

                String newKey = placeKey(np);
                globalUsed.add(newKey);
                planKeys.add(newKey);
                if (isThemeItem && needThemeReplacement) {
                    // 유지
                } else if (isThemeItem && !matchesTheme(np, themeKw)) {
                    themeCount = Math.max(0, themeCount - 1);
                } else if (!isThemeItem && matchesTheme(np, themeKw)) {
                    themeCount++;
                }
            }
        }
    }

    private PlaceDto pickReplacementCandidate(List<PlaceDto> seeds,
                                              Set<String> globalUsed,
                                              Set<String> planKeys,
                                              boolean needTheme,
                                              String themeKw) {
        for (PlaceDto s : seeds) {
            String key = placeKey(s);
            if (key.isBlank()) continue;
            if (globalUsed.contains(key)) continue;
            if (planKeys.contains(key)) continue;

            String name = Optional.ofNullable(s.getPlaceName()).orElse("");
            if (needTheme && !name.contains(Optional.ofNullable(themeKw).orElse(""))) continue;

            return s;
        }
        return null;
    }

    private PlaceDto pickAdditionalCandidateForPlan(List<PlanItemDto> items,
                                                    List<PlaceDto> seeds) {
        Set<String> planKeys = items.stream()
                .filter(PlaceItemDto.class::isInstance)
                .map(PlaceItemDto.class::cast)
                .map(this::placeKey)
                .filter(k -> !k.isBlank())
                .collect(Collectors.toSet());

        for (PlaceDto s : seeds) {
            String key = placeKey(s);
            if (key.isBlank()) continue;
            if (planKeys.contains(key)) continue;
            return s; // 첫 미사용 seed
        }
        return null;
    }

    private PlaceItemDto createPlaceFromSeed(PlaceDto seed,
                                             LocalTime start,
                                             String duration,
                                             String title,
                                             String descIfEmpty) {
        String desc = Optional.ofNullable(descIfEmpty).orElse("장소 방문");
        Integer estimated = estimatePlaceCost(seed, seed.getPlaceName(), desc);
        Integer cost = enforcePlaceMinCost(seed.getPlaceName(), desc, estimated); // 최소 비용 보정
        int d = Math.min(parseMinutes(duration), 150); // duration cap
        PlaceItemDto np = PlaceItemDto.of(
                title,
                0,
                cost,
                String.valueOf(Math.max(0, d)),
                start,
                desc,
                seed.getPlaceUrl(),
                seed.getPlaceName()
        );
        try { np.getClass().getMethod("setPlaceName", String.class).invoke(np, seed.getPlaceName()); } catch (Exception ignore) {}
        return np;
    }

    private boolean matchesTheme(PlaceItemDto p, String themeKw) {
        if (themeKw == null || themeKw.isBlank()) return false;
        String pn = safeStr(callGetter(p, "getPlaceName"));
        String ti = safeStr(p.getTitle());
        String ds = safeStr(p.getDescription());
        return pn.contains(themeKw) || ti.contains(themeKw) || ds.contains(themeKw);
    }

    private String placeKey(PlaceItemDto p) {
        String url = safeStr(callGetter(p, "getPlaceUrl"));
        String name = safeStr(callGetter(p, "getPlaceName"));
        if (!url.isBlank()) return "URL:" + url;
        if (!name.isBlank()) return "NAME:" + name;
        return "";
    }
    private String placeKey(PlaceDto s) {
        String url = Optional.ofNullable(s.getPlaceUrl()).orElse("");
        String name = Optional.ofNullable(s.getPlaceName()).orElse("");
        if (!url.isBlank()) return "URL:" + url;
        if (!name.isBlank()) return "NAME:" + name;
        return "";
    }

    // ---------- 테마 장소 보장 ----------
    private void ensureThemePlace(List<PlanItemDto> items, String themeKw,
                                  List<PlaceDto> seedList,
                                  Map<String, PlaceDto> seedByUrl) {
        if (themeKw == null || themeKw.isBlank()) return;

        boolean hasTheme = items.stream().anyMatch(pi -> {
            if (!(pi instanceof PlaceItemDto p)) return false;
            String pn = safeStr(callGetter(p, "getPlaceName"));
            String ti = safeStr(p.getTitle());
            String ds = safeStr(p.getDescription());
            return pn.contains(themeKw) || ti.contains(themeKw) || ds.contains(themeKw);
        });
        if (hasTheme) return;

        Set<String> usedUrls = items.stream()
                .filter(PlaceItemDto.class::isInstance)
                .map(PlaceItemDto.class::cast)
                .map(p -> safeStr(callGetter(p, "getPlaceUrl")))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());

        PlaceDto candidate = seedList.stream()
                .filter(s -> Optional.ofNullable(s.getPlaceName()).orElse("").contains(themeKw))
                .filter(s -> s.getPlaceUrl() != null && !usedUrls.contains(s.getPlaceUrl()))
                .findFirst().orElse(null);

        if (candidate == null) {
            log.warn("테마({})에 맞는 seed 장소를 찾지 못했습니다.", themeKw);
            return;
        }

        int idx = -1;
        for (int i = items.size()-1; i >= 0; i--) {
            if (items.get(i) instanceof PlaceItemDto) { idx = i; break; }
        }
        if (idx < 0) return;

        PlaceItemDto oldP = (PlaceItemDto) items.get(idx);
        LocalTime st = oldP.getStartTime();
        String dur = Optional.ofNullable(oldP.getDuration()).orElse("60");
        String desc = Optional.ofNullable(oldP.getDescription()).orElse("");

        PlaceItemDto np = PlaceItemDto.of(
                candidate.getPlaceName() + " 방문",
                0,
                enforcePlaceMinCost(candidate.getPlaceName(), desc, estimatePlaceCost(candidate, candidate.getPlaceName(), desc)),
                dur,
                st,
                desc.isBlank() ? "테마 관련 장소 방문" : desc,
                candidate.getPlaceUrl(),
                candidate.getPlaceName()
        );
        try { np.getClass().getMethod("setPlaceName", String.class).invoke(np, candidate.getPlaceName()); } catch (Exception ignore) {}

        items.set(idx, np);
    }

    // ---------- 플랜 내 장소 중복 제거 ----------
    private List<PlanItemDto> dedupePlacesInPlan(List<PlanItemDto> items) {
        Set<String> seen = new HashSet<>();
        List<PlanItemDto> out = new ArrayList<>();
        for (PlanItemDto it : items) {
            if (it instanceof PlaceItemDto p) {
                String key = placeKey(p);
                if (key.isBlank()) { out.add(it); continue; }
                if (seen.contains(key)) continue;
                seen.add(key);
            }
            out.add(it);
        }
        return out;
    }

    // ---------- duration cap ----------
    private void capPlaceDurations(List<PlanItemDto> items, int maxMinutes) {
        for (PlanItemDto it : items) {
            if (it instanceof PlaceItemDto) {
                int d = Math.min(parseMinutes(it.getDuration()), maxMinutes);
                it.setDuration(String.valueOf(Math.max(0, d)));
            }
        }
    }

    // ---------- 끝에 장소 하나 추가해서 시간 채우기 ----------
    private void appendExtraPlaceIfEndGapBig(List<PlanItemDto> items,
                                             LocalTime ttEnd,
                                             List<PlaceDto> seeds,
                                             String themeKw) {
        if (items == null || items.isEmpty() || ttEnd == null) return;

        LocalTime curEnd = parseTime(computePlanEndTime(items));
        if (curEnd == null) return;

        int gap = (int) ChronoUnit.MINUTES.between(curEnd, ttEnd);
        if (gap <= 45) return; // 의미 있는 추가는 45분 초과일 때만

        PlaceDto cand = pickAdditionalCandidateForPlan(items, seeds);
        if (cand == null) return;

        int dur = Math.max(60, Math.min(120, gap - 30));
        PlaceItemDto np = createPlaceFromSeed(
                cand,
                curEnd,
                String.valueOf(dur),
                Optional.ofNullable(cand.getPlaceName()).orElse("장소") + " 방문",
                "추가 방문지"
        );

        items.add(np);
        items.sort(Comparator.comparing(PlanItemDto::getStartTime, Comparator.nullsFirst(Comparator.naturalOrder())));
    }

    // ---------- endTime 계산 ----------
    private String computePlanEndTime(List<PlanItemDto> items) {
        LocalTime maxEnd = null;
        for (PlanItemDto it : items) {
            LocalTime st = it.getStartTime();
            if (st == null) continue;
            int mins = parseMinutes(it.getDuration());
            LocalTime end = st.plusMinutes(Math.max(0, mins));
            if (maxEnd == null || end.isAfter(maxEnd)) maxEnd = end;
        }
        return formatHm(maxEnd);
    }

    // ===== PLACE 최소 비용 보정 =====
    private static final int MIN_PLACE_COST = 1000;
    private static final int MIN_CAFE_COST  = 8000;

    private int enforcePlaceMinCost(String placeName, String desc, Integer cost) {
        int c = cost == null ? 0 : cost;
        String s = (safeStr(placeName) + " " + safeStr(desc)).toLowerCase();
        boolean isCafe = containsAny(s, "카페","커피","디저트","브런치","스타벅스","tea");
        return isCafe ? Math.max(c, MIN_CAFE_COST) : Math.max(c, MIN_PLACE_COST);
    }

    // -------------------- PLACE 비용 추정(보조) --------------------
    private Integer estimatePlaceCost(PlaceDto seed, String placeName, String desc) {
        String s = (safeStr(placeName) + " " + safeStr(desc)).toLowerCase();
        if (s.contains("무료") || s.contains("free")) return 0;
        if (containsAny(s, "박물관","미술관","전시")) return 12000;
        if (containsAny(s, "공연","연극","콘서트","뮤지컬")) return 30000;
        if (containsAny(s, "체험","공방","키트")) return 15000;
        if (containsAny(s, "테마파크","놀이공원")) return 35000;
        if (containsAny(s, "전망대","스카이","타워")) return 10000;
        if (containsAny(s, "카페","디저트")) return 8000;
        if (containsAny(s, "쇼핑","백화점","몰","마켓")) return 0;
        return 0;
    }
    private boolean containsAny(String s, String... kws) { for (String k: kws) if (s.contains(k)) return true; return false; }

    // -------------------- 유틸 --------------------
    private LocalTime parseTime(String hhmm) { try { return LocalTime.parse(hhmm); } catch (Exception e) { return LocalTime.MIDNIGHT; } }
    private int parseMinutes(String m) { try { return Integer.parseInt(Optional.ofNullable(m).orElse("0")); } catch (Exception e) { return 0; } }
    private String formatHm(LocalTime t) { return t == null ? null : String.format("%02d:%02d", t.getHour(), t.getMinute()); }
    private String extractPayload(JsonNode root) {
        if (root == null) return null;
        if (root.has("output") && root.get("output").isArray() && root.get("output").size() > 0) {
            JsonNode msg = root.get("output").get(0);
            JsonNode content = msg.get("content");
            if (content != null && content.isArray()) {
                for (JsonNode c : content) {
                    if (c.hasNonNull("text")) return c.get("text").asText();
                    if (c.hasNonNull("output_text")) return c.get("output_text").asText();
                }
            } else if (content != null && content.isTextual()) {
                return content.asText();
            }
        }
        return null;
    }
    private String safeStr(Object s) { return s == null ? "" : s.toString(); }
    private Object callGetter(Object obj, String getter) { try { Method m = obj.getClass().getMethod(getter); return m.invoke(obj); } catch (Exception e) { return null; } }

    // ---------- 이동 정책 강제 ----------
    private void enforceMovePolicy(List<PlanItemDto> items, boolean isOneWay, LocalTime ttStart, LocalTime ttEnd) {
        if (items == null || items.isEmpty()) return;

        int firstPlaceIdx = -1, lastPlaceIdx = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof PlaceItemDto) {
                if (firstPlaceIdx < 0) firstPlaceIdx = i;
                lastPlaceIdx = i;
            }
        }
        if (firstPlaceIdx < 0) {
            items.removeIf(this::isMoveItem);
            return;
        }

        List<PlanItemDto> existingMoves = items.stream()
                .filter(this::isMoveItem)
                .sorted(Comparator.comparing(PlanItemDto::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        PlanItemDto depProto = existingMoves.isEmpty() ? null : existingMoves.get(0);
        PlanItemDto retProto = existingMoves.isEmpty() ? null : existingMoves.get(existingMoves.size()-1);

        items.removeIf(this::isMoveItem);

        firstPlaceIdx = -1; lastPlaceIdx = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof PlaceItemDto) {
                if (firstPlaceIdx < 0) firstPlaceIdx = i;
                lastPlaceIdx = i;
            }
        }
        if (firstPlaceIdx < 0) return;

        PlaceItemDto firstPlace = (PlaceItemDto) items.get(firstPlaceIdx);
        PlaceItemDto lastPlace  = (PlaceItemDto) items.get(lastPlaceIdx);

        LocalTime firstStart = firstPlace.getStartTime();
        LocalTime lastEnd    = lastPlace.getStartTime() == null
                ? null
                : lastPlace.getStartTime().plusMinutes(Math.max(0, parseMinutes(lastPlace.getDuration())));

        MoveItemDto depMove = makeDepartureMove(depProto, firstStart, ttStart);
        items.add(firstPlaceIdx, depMove);

        if (!isOneWay && lastEnd != null) {
            lastPlaceIdx = -1;
            for (int i = 0; i < items.size(); i++) if (items.get(i) instanceof PlaceItemDto) lastPlaceIdx = i;
            lastPlace = (PlaceItemDto) items.get(lastPlaceIdx);
            lastEnd   = lastPlace.getStartTime().plusMinutes(Math.max(0, parseMinutes(lastPlace.getDuration())));

            MoveItemDto retMove = makeReturnMove(retProto, lastEnd, ttEnd);
            items.add(lastPlaceIdx + 1, retMove);
        }

        items.sort(Comparator.comparing(PlanItemDto::getStartTime, Comparator.nullsFirst(Comparator.naturalOrder())));
    }

    private boolean isMoveItem(PlanItemDto it) {
        String t = it.getTitle() == null ? "" : it.getTitle().trim();
        return "이동".equals(t);
    }

    private MoveItemDto makeDepartureMove(PlanItemDto proto, LocalTime firstStart, LocalTime ttStart) {
        int dur  = proto == null ? 12 : parseMinutes(proto.getDuration());
        int cost = proto == null ? 0  : Optional.ofNullable((Integer) callGetter(proto, "getCost")).orElse(0);
        dur = Math.max(5, dur);

        LocalTime start = firstStart == null ? ttStart : firstStart.minusMinutes(Math.max(0, dur));
        if (ttStart != null && start != null && start.isBefore(ttStart) && firstStart != null) {
            start = ttStart;
            dur = (int) Math.max(1, ChronoUnit.MINUTES.between(start, firstStart));
        }
        if (start == null) start = ttStart != null ? ttStart : LocalTime.MIDNIGHT;

        return MoveItemDto.of("이동", 0, cost, String.valueOf(dur), start);
    }

    private MoveItemDto makeReturnMove(PlanItemDto proto, LocalTime lastEnd, LocalTime ttEnd) {
        int dur  = proto == null ? 12 : parseMinutes(proto.getDuration());
        int cost = proto == null ? 0  : Optional.ofNullable((Integer) callGetter(proto, "getCost")).orElse(0);
        dur = Math.max(5, dur);

        LocalTime start = lastEnd == null ? LocalTime.MIDNIGHT : lastEnd;
        if (ttEnd != null && start != null) {
            int cap = (int) ChronoUnit.MINUTES.between(start, ttEnd);
            if (cap >= 0) dur = Math.max(1, Math.min(dur, cap));
        }
        return MoveItemDto.of("이동", 0, cost, String.valueOf(dur), start);
    }

    /**
     * 타임테이블 종료시각(ttEnd) 대비 남는 시간이 30분을 초과하면,
     * 마지막 PLACE(우선) 또는 마지막 비이동 아이템의 duration을 늘려서
     * 남는 시간을 30분 이하로 맞춘다. (이미 끝에 장소 추가 시 남는 자투리만 보정)
     */
    private void stretchPlanToTimeTable(List<PlanItemDto> items, LocalTime ttEnd) {
        if (items == null || items.isEmpty() || ttEnd == null) return;

        String curEndStr = computePlanEndTime(items);
        LocalTime curEnd = parseTime(curEndStr);
        if (curEnd == null) return;

        int delta = (int) ChronoUnit.MINUTES.between(curEnd, ttEnd); // curEnd -> ttEnd
        int targetGap = 30;
        int extendBy = delta - targetGap;

        if (extendBy <= 0) return;

        int idxToExtend = -1;
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i) instanceof PlaceItemDto) { idxToExtend = i; break; }
        }
        if (idxToExtend < 0) {
            for (int i = items.size() - 1; i >= 0; i--) {
                String ti = items.get(i).getTitle() == null ? "" : items.get(i).getTitle();
                if (!"이동".equals(ti)) { idxToExtend = i; break; }
            }
        }
        if (idxToExtend < 0) return;

        PlanItemDto extend = items.get(idxToExtend);
        int baseDur = parseMinutes(extend.getDuration());
        extend.setDuration(String.valueOf(Math.max(0, baseDur + extendBy)));

        for (int k = idxToExtend + 1; k < items.size(); k++) {
            PlanItemDto it = items.get(k);
            LocalTime st = it.getStartTime();
            if (st != null) it.setStartTime(st.plusMinutes(extendBy));
        }
    }

    // ✅ 식사 시간대 강제: 시작시간을 식사 창(타임테이블과의 교집합) 안으로 이동, 교집합 없으면 제거
    private void enforceMealWindows(List<PlanItemDto> items, LocalTime ttStart, LocalTime ttEnd) {
        if (items == null || items.isEmpty()) return;

        List<PlanItemDto> toRemove = new ArrayList<>();
        for (PlanItemDto it : items) {
            String title = Optional.ofNullable(it.getTitle()).orElse("");
            if (!title.contains("식사")) continue;

            LocalTime winS = null, winE = null;
            if (title.contains("아침")) { winS = BF_START; winE = BF_END; }
            else if (title.contains("점심")) { winS = LU_START; winE = LU_END; }
            else if (title.contains("저녁")) { winS = DN_START; winE = DN_END; }
            else continue;

            LocalTime allowedS = maxTime(winS, ttStart);
            LocalTime allowedE = minTime(winE, ttEnd);
            if (allowedS == null || allowedE == null || allowedS.isAfter(allowedE)) {
                // 타임테이블과 교집합이 없으면 해당 식사 제거
                toRemove.add(it);
                continue;
            }

            LocalTime cur = it.getStartTime();
            if (cur == null || cur.isBefore(allowedS) || cur.isAfter(allowedE)) {
                it.setStartTime(allowedS);
            }
            // 식사는 항상 60분 유지
            it.setDuration("60");
        }

        if (!toRemove.isEmpty()) items.removeAll(toRemove);

        // 정렬 정리
        items.sort(Comparator.comparing(PlanItemDto::getStartTime, Comparator.nullsFirst(Comparator.naturalOrder())));
    }

    // 식사 창 상수
    private static final LocalTime BF_START = LocalTime.of(8, 0);
    private static final LocalTime BF_END   = LocalTime.of(10, 59);
    private static final LocalTime LU_START = LocalTime.of(11, 0);
    private static final LocalTime LU_END   = LocalTime.of(15, 29);
    private static final LocalTime DN_START = LocalTime.of(17, 30);
    private static final LocalTime DN_END   = LocalTime.of(21, 59);

    private LocalTime maxTime(LocalTime a, LocalTime b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }
    private LocalTime minTime(LocalTime a, LocalTime b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isBefore(b) ? a : b;
    }

    // -------------------- 모델 응답 파싱용 DTO --------------------
    @Data @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GenItem {
        private String title;
        private String duration;
        @JsonProperty("start_time") private String startTime;
        @JsonProperty("order_num") private Integer orderNum;
        private Integer cost;

        @JsonProperty("place_url") private String placeUrl;
        private String placeName;
        private String description;
    }

    @Data @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GenPlan {
        private String order;
        private List<GenItem> planItemDtos;
    }

    @Data @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GenRoot {
        private List<GenPlan> planDtos;
    }

}
