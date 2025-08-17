package pard.server.com.nadri.plan.dto.res;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalTime;

@AllArgsConstructor @NoArgsConstructor @SuperBuilder
@Getter @Setter
public abstract class PlanItemDto {
    private String title; // 제목: 식사일 경우에 아침식사 / 점심 식사 / 저녁 식사 / 셋중 하나,
    // 이동일 경우에 버스 이동 / 지하철 이동 / 자차 이동 / 도보 이동 넷중 하나.
    private int orderNum; // 일정 순서
    private LocalTime startTime; // 시작 시간
    private LocalTime endTime; // 끝 시간
    private String duration; // 소요 시간 (식사시간 경우에 소요시간 60분 고정)
    private int cost; // 비용 (식사시간 경우에 그냥 null 해두기)
}
