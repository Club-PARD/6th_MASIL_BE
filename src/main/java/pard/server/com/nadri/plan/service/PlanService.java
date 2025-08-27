package pard.server.com.nadri.plan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pard.server.com.nadri.kakaoLocal.dto.Coord;
import pard.server.com.nadri.kakaoLocal.dto.PlaceDto;
import pard.server.com.nadri.kakaoLocal.service.KakaoLocalService;
import pard.server.com.nadri.openai.dto.*;
import pard.server.com.nadri.openai.service.OpenAiService;
import pard.server.com.nadri.plan.dto.req.CreatePlanDto;
import pard.server.com.nadri.plan.dto.res.*;
import pard.server.com.nadri.plan.entity.*;
import pard.server.com.nadri.plan.repository.PlanItemRepo;
import pard.server.com.nadri.plan.repository.PlanRepo;
import pard.server.com.nadri.plan.repository.PlansRepo;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanService {
    private final KakaoLocalService kakaoLocalService;
    private final OpenAiService openAiService;
    private final PlanRepo planRepo;
    private final PlanItemRepo planItemRepo;
    private final PlansRepo plansRepo;

    @Transactional
    public ResponsePlansDto createPlan(CreatePlanDto createPlanDto) {
        Coord coord = kakaoLocalService.convertToCoordinate(createPlanDto.getOrigin());
        List<String> codes = List.of("CT1", "AT4", "CE7");
        List<PlaceDto> placeDtos = kakaoLocalService.searchByCategories(coord, codes);
        PlansDto plansDto = openAiService.callChatApi(createPlanDto, coord ,placeDtos);

        // Plans 빼주고, 그거 바탕으로 ResponsePlansDto 만들기
        Plans plans = savePlans(plansDto);
        return getPlansDto(plans.getId());
    }

    @Transactional
    public Plans savePlans(PlansDto plansDto){
        Plans plans = new Plans();

        plansDto.getPlanDtos().forEach(planDto -> {
            Plan plan = Plan.from(planDto);
            plan.savePlans(plans);
            plans.getPlans().add(plan);
        });

        return plansRepo.save(plans);
    }

    public ResponsePlansDto getPlansDto(Long plansId){
        Plans plans = plansRepo.findById(plansId).orElseThrow();

        // plans에 끼워넣을 planList
        List<ResponsePlanDto> responsePlanDtos= new ArrayList<>();
        plans.getPlans().forEach(plan -> {
            // plan에 끼워넣을 itemList
            List<ResponseItemDto> responseItemDtos = new ArrayList<>();

            plan.getPlanItems().forEach(planItem -> {
                ResponseItemDto responseItemDto = ResponseItemDto
                        .builder()
                        .title(planItem.getTitle())
                        .orderNum(planItem.getOrderNum())
                        .build();

                // item 만들었으면 itemList에 add.
                responseItemDtos.add(responseItemDto);
            });

            ResponsePlanDto responsePlanDto = ResponsePlanDto
                    .builder().planId(plan.getId())
                    .order(plan.getOrder())
                    .endTime(plan.getEndTime())
                    .itemDtos(responseItemDtos) // 이번 턴의 plan의 완성된 itemList들을 끼워넣기
                    .build();

            responsePlanDtos.add(responsePlanDto); // Plan 만들었으면 PlanList에 추가
        });
        return ResponsePlansDto.builder()
                .responsePlanDtos(responsePlanDtos) // planList 완성됐으면 plans에 추가
                .plansId(plans.getId())
                .build();
    }

    public PlanDetailsDto getDetails(Long planId) {
        Plan plan = planRepo.findById(planId).orElseThrow();
        List<PlanItem> planItems = planItemRepo.findAllByPlan(plan);

        List<PlanItemDto> planItemDtos = planItems.stream().map(this::toItemDto).toList();

        return PlanDetailsDto.builder()
                .itemDtos(planItemDtos)
                .build();
    }

    public PlanItemDto toItemDto(PlanItem planItem) {
        if (planItem instanceof MealItem m) {
            return MealItemDto.of(m.getTitle(), m.getOrderNum(), "60", m.getStartTime());
        } else if (planItem instanceof MoveItem mv) {
            return MoveItemDto.of(mv.getTitle(), mv.getOrderNum(), mv.getCost(), mv.getDuration(), mv.getStartTime());
        } else if (planItem instanceof PlaceItem p) {
            return PlaceItemDto.of(p.getTitle(), p.getOrderNum(), p.getCost(), p.getDuration(), p.getStartTime(), p.getDescription(), p.getLinkUrl(),p.getPlaceName());
            }
        {
            throw new IllegalStateException("Unknown PlanItem subtype: " + planItem.getClass());
        }
    }
}
