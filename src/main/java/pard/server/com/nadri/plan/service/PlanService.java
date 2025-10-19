package pard.server.com.nadri.plan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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
    private final PlanUtil planUtil;

    @Transactional
    public ResponsePlansDto createPlan(CreatePlanDto createPlanDto) throws JsonProcessingException {
        Coord coord = kakaoLocalService.convertToCoordinate(createPlanDto.getOrigin());
        List<String> keywords = planUtil.makeKeywords(createPlanDto.getTheme());
        List<PlaceDto> placeDtos = kakaoLocalService.searchByKeywords(coord, keywords);
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

        // Plans로 List<ResponsePlanDto> 만들기
        List<ResponsePlanDto> responsePlanDtos = planUtil.getPlansDtos(plans);

        return ResponsePlansDto.of(plans.getId(), responsePlanDtos);
    }

    public PlanDetailsDto getDetails(Long planId) {
        Plan plan = planRepo.findById(planId).orElseThrow();
        List<PlanItem> planItems = planItemRepo.findAllByPlan(plan);

        // PlanItem들로 각각 Dto 만들어서 PlanDetailsDto에 쏙 넣어주기.
        List<PlanItemDetailsDto> planItemDetailsDtos = planItems.stream().map(planUtil::toItemDto).toList();

        return PlanDetailsDto.from(planItemDetailsDtos);
    }
}
