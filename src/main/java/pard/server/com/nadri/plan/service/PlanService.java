package pard.server.com.nadri.plan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pard.server.com.nadri.kakaoLocal.dto.Coord;
import pard.server.com.nadri.kakaoLocal.dto.PlaceDto;
import pard.server.com.nadri.kakaoLocal.service.KakaoLocalService;
import pard.server.com.nadri.openai.dto.ResponsePlansDto;
import pard.server.com.nadri.openai.service.OpenAiService;
import pard.server.com.nadri.plan.dto.req.CreatePlanDto;
import pard.server.com.nadri.plan.dto.res.*;
import pard.server.com.nadri.plan.entity.*;
import pard.server.com.nadri.plan.repository.PlanItemRepo;
import pard.server.com.nadri.plan.repository.PlanRepo;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanService {
    private final KakaoLocalService kakaoLocalService;
    private final OpenAiService openAiService;
    private final PlanRepo planRepo;
    private final PlanItemRepo planItemRepo;
    private final ObjectMapper om = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public void createPlan(CreatePlanDto createPlanDto) {
        Coord coord = kakaoLocalService.convertToCoordinate(createPlanDto.getOrigin());
        List<String> codes = List.of("CT1", "AT4", "CE7");
        List<PlaceDto> placeDtos = kakaoLocalService.searchByCategories(coord, codes);

        try {
            log.info("placeDtos =>\n{}", om.writeValueAsString(placeDtos));
        } catch (Exception e) {
            log.warn("log print failed", e);
        }
//        return openAiService.callChatApi(placeDtos);
    }

    public PlanDetailsDto getDetails(Long planId) {
        Plan plan = planRepo.findById(planId).orElseThrow();
        List<PlanItem> planItems = planItemRepo.findAllByPlan(plan);

        List<PlanItemDto> planItemDtos = planItems.stream().map(this::toItemDto).toList();

        return PlanDetailsDto.builder()
                .itemDtos(planItemDtos).build();

    }

    public PlanItemDto toItemDto(PlanItem planItem) {
        if (planItem instanceof MealItem m) {
            return MealItemDto.builder()
                    .title(planItem.getTitle())
                    .orderNum(planItem.getOrderNum())
                    .duration("60분")
                    .startTime(planItem.getStartTime())
                    .endTime(planItem.getEndTime())
                    .build();
        } else if (planItem instanceof MoveItem mv) {
            return MoveItemDto.builder()
                    .title(planItem.getTitle())
                    .orderNum(planItem.getOrderNum())
                    .duration(planItem.getDuration())
                    .startTime(planItem.getStartTime())
                    .endTime(planItem.getEndTime())
                    .cost(planItem.getCost())
                    .build();
        } else if (planItem instanceof PlaceItem p) {
            return PlaceItemDto.builder()
                    .title(planItem.getTitle())
                    .orderNum(planItem.getOrderNum())
                    .duration(planItem.getDuration())
                    .startTime(planItem.getStartTime())
                    .endTime(planItem.getEndTime())
                    .cost(planItem.getCost())
                    .linkUrl(p.getLinkUrl())
                    .description(p.getDescription())
                    .build();
            }
        {
            throw new IllegalStateException("Unknown PlanItem subtype: " + planItem.getClass());
        }
    }
}
