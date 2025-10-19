package pard.server.com.nadri.plan.dto.res;

import lombok.*;
import pard.server.com.nadri.openai.dto.PlanItemDetailsDto;

import java.util.List;

@AllArgsConstructor @NoArgsConstructor @Builder
@Getter @Setter
public class PlanDetailsDto {
    private List<PlanItemDetailsDto> itemDetailsDtos;

    public static PlanDetailsDto from(List<PlanItemDetailsDto> planItemDetailsDtos) {
        return PlanDetailsDto.builder()
                .itemDetailsDtos(planItemDetailsDtos)
                .build();
    }
}
