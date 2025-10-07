package pard.server.com.nadri.plan.dto.res;

import lombok.*;
import pard.server.com.nadri.plan.entity.Plan;

import java.time.LocalTime;
import java.util.List;

@AllArgsConstructor @NoArgsConstructor @Builder
@Getter @Setter
public class ResponsePlanDto { // 프론트에 보내줘야 되는 Plan DTO
    private Long planId;
    private String order;
    private List<ResponseItemDto> itemDtos;

    public static ResponsePlanDto from(Plan plan, List<ResponseItemDto> responseItemDtos){
        return ResponsePlanDto.builder().planId(plan.getId())
                .order(plan.getOrder())
                .itemDtos(responseItemDtos) // 이번 턴의 plan의 완성된 itemList들을 끼워넣기
                .build();
    }
}
