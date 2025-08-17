package pard.server.com.nadri.plan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import pard.server.com.nadri.kakaoLocal.service.KakaoLocalService;
import pard.server.com.nadri.openai.OpenAiProps;
import pard.server.com.nadri.openai.service.OpenAiService;
import pard.server.com.nadri.plan.dto.req.CreatePlanDto;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanService {
    private final KakaoLocalService kakaoLocalService;
    private final OpenAiService openAiService;

    public void createPlan(CreatePlanDto createPlanDto) {
        kakaoLocalService.convertToCoordinate(createPlanDto);
        kakaoLocalService.searchByCategory();
        openAiService.callChatApi();
    }
}
