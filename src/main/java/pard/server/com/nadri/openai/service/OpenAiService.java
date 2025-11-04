package pard.server.com.nadri.openai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import pard.server.com.nadri.kakaoLocal.dto.Coord;
import pard.server.com.nadri.kakaoLocal.dto.PlaceDto;
import pard.server.com.nadri.openai.dto.*;
import pard.server.com.nadri.plan.dto.req.CreatePlanDto;

import java.util.*;

@Service
@Slf4j
public class OpenAiService {

    @Qualifier("openAiWebClient")
    private final WebClient client;
    private final ObjectMapper mapper;
    private final JsonPromptUtil jsonPromptUtil;

    public OpenAiService(@Qualifier("openAiWebClient") WebClient client, ObjectMapper mapper, JsonPromptUtil jsonPromptUtil) {
        this.client = client;
        this.mapper = mapper.registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);;
        this.jsonPromptUtil = jsonPromptUtil;
    }

    /** 메인: 좌표/테마/식사/이동 규칙 포함해서 3개의 Plan 생성 */
    public PlansDto callChatApi(CreatePlanDto req, Coord coord, List<PlaceDto> placeDtos) throws JsonProcessingException {
        System.out.println("callChatApi");
        // seed 직렬화 + 조회 맵
        final String seedJson;
        try {
            seedJson = mapper.writeValueAsString(placeDtos);
        } catch (Exception e) {
            throw new RuntimeException("placeDtos 직렬화 실패", e);
        }

        //  JSON 스키마
        Map<String, Object> rootSchema = jsonPromptUtil.getRootSchema();

        //  호출
        String prompt = jsonPromptUtil.getPrompt(req, coord, seedJson);

        //  response 받아오기
        String responseJson = jsonPromptUtil.getResponseJson(prompt, rootSchema, client);

        // response로 PlansDto 만들어서 반환
        return jsonPromptUtil.getPlansDto(responseJson, mapper);
    }
}
