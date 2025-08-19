package pard.server.com.nadri.openai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class PlanDto {
    private String order;

    @Builder.Default
    private List<PlanItemDto> planItemDtos = new ArrayList<>();

    /** 플랜 마지막 아이템 종료 시각 (HH:mm) */
    private String endTime;

    /** 편의 생성자: endTime은 일단 null */
    public PlanDto(String order, List<PlanItemDto> planItemDtos) {
        this.order = order;
        this.planItemDtos = (planItemDtos != null) ? planItemDtos : new ArrayList<>();
        this.endTime = null;
    }

    /** 편의 팩토리 */
    public static PlanDto of(String order, List<PlanItemDto> planItemDtos) {
        return new PlanDto(order, planItemDtos);
    }
}
