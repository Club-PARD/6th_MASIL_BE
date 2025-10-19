package pard.server.com.nadri.plan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pard.server.com.nadri.openai.dto.PlanItemDetailsDto;
import pard.server.com.nadri.plan.dto.res.ResponseItemDto;
import pard.server.com.nadri.plan.dto.res.ResponsePlanDto;
import pard.server.com.nadri.plan.entity.PlanItem;
import pard.server.com.nadri.plan.entity.Plans;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanUtil {
    public List<String> makeKeywords(String theme){
        return switch (theme) {
            case "탐방" -> List.of("박물관", "축제");
            case "체험" -> List.of("원데이 클래스", "공방");
            case "관광" -> List.of("관광", "자연");
            case "쇼핑" -> List.of("백화점", "소품점");
            default -> null;
        };
    }

    public List<ResponsePlanDto> getPlansDtos(Plans plans){

        // plans에 끼워넣을 planList
        List<ResponsePlanDto> responsePlanDtos = new ArrayList<>();
        plans.getPlans().forEach(plan -> {
            // plan에 끼워넣을 itemList
            List<ResponseItemDto> responseItemDtos = new ArrayList<>();

            plan.getPlanItems().forEach(planItem -> {
                ResponseItemDto responseItemDto = ResponseItemDto.from(planItem);

                // item 만들었으면 itemList에 add.
                responseItemDtos.add(responseItemDto);
            });

            ResponsePlanDto responsePlanDto = ResponsePlanDto.from(plan, responseItemDtos) ;

            responsePlanDtos.add(responsePlanDto); // Plan 만들었으면 PlanList에 추가
        });

        return responsePlanDtos;
    }

    public PlanItemDetailsDto toItemDto(PlanItem planItem) {
        if (planItem instanceof PlanItem.MealItem m) {
            return PlanItemDetailsDto.MealItemDetailsDto.of(m.getTitle(), m.getOrderNum(), "60", m.getStartTime());
        } else if (planItem instanceof PlanItem.MoveItem mv) {
            return PlanItemDetailsDto.MoveItemDetailsDto.of(mv.getTitle(), mv.getOrderNum(),mv.getDuration(), mv.getStartTime());
        } else if (planItem instanceof PlanItem.PlaceItem p) {
            return PlanItemDetailsDto.PlaceItemDetailsDto.of(p.getTitle(), p.getOrderNum(), p.getCost(), p.getDuration(), p.getStartTime(), p.getDescription(), p.getLinkUrl(),p.getPlaceName());
        }
        {
            throw new IllegalStateException("Unknown PlanItem subtype: " + planItem.getClass());
        }
    }
}
