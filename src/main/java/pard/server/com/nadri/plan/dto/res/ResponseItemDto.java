package pard.server.com.nadri.plan.dto.res;

import lombok.*;
import pard.server.com.nadri.plan.entity.PlanItem;

@AllArgsConstructor @NoArgsConstructor @Builder
@Getter @Setter
public class ResponseItemDto { // 프론트에 보내줘야 되는 PlanItem DTO
    private String title; // 제목: 식사일 경우에 아침식사 / 점심 식사 / 저녁 식사 / 셋중 하나,
                          // 이동일 경우에 버스 이동 / 지하철 이동 / 자차 이동 / 도보 이동 넷중 하나.
    private int orderNum; // 일정 순서

    public static ResponseItemDto from(PlanItem planItem){
        return ResponseItemDto.builder()
                .title(planItem.getTitle())
                .orderNum(planItem.getOrderNum())
                .build();
    }
}
