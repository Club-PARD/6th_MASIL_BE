package pard.server.com.nadri.openai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.SuperBuilder;
import pard.server.com.nadri.plan.entity.MealItem;
import pard.server.com.nadri.plan.entity.MoveItem;
import pard.server.com.nadri.plan.entity.PlaceItem;
import pard.server.com.nadri.plan.entity.PlanItem;

import java.time.LocalTime;

@AllArgsConstructor @NoArgsConstructor @SuperBuilder
@Getter @Setter
public abstract class PlanItemDto { // 지피티가 만들 가장 최소단위 dto, 이걸로 PlansItem 만들고 그걸로 save.
    private String title; // 제목: 식사일 경우에 아침식사 / 점심 식사 / 저녁 식사 / 셋중 하나,
    // 이동일 경우에 버스 이동 / 지하철 이동 / 자차 이동 / 도보 이동 넷중 하나.

    @JsonProperty("order_num")
    private int orderNum; // 일정 순서

    @JsonProperty("start_time")
    @JsonFormat(pattern = "HH:mm")   // 문자열 "HH:mm"을 LocalTime으로
    private LocalTime startTime; // 시작 시간
    private String duration; // 소요 시간 (식사시간 경우에 소요시간 60분 고정)
    private Integer cost; // 비용 (식사시간 경우에 그냥 null 해두기)
}
