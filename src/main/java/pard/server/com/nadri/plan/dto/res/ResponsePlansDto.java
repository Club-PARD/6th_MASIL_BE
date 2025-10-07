package pard.server.com.nadri.plan.dto.res;

import lombok.*;
import pard.server.com.nadri.plan.entity.Plan;
import pard.server.com.nadri.plan.entity.Plans;

import java.util.List;

@AllArgsConstructor @NoArgsConstructor @Builder
@Getter @Setter
public class ResponsePlansDto { // 프론트에 보내줘야되는 PLans DTO
    private List<ResponsePlanDto> responsePlanDtos;
    private Long plansId;

    public static ResponsePlansDto of(Long plansId, List<ResponsePlanDto> responsePlanDtos) {
        return ResponsePlansDto.builder()
                .responsePlanDtos(responsePlanDtos) // planList 완성됐으면 plans에 추가
                .plansId(plansId)
                .build();
    }
}
