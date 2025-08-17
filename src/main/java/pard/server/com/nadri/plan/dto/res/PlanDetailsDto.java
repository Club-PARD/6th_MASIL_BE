package pard.server.com.nadri.plan.dto.res;

import lombok.*;

import java.util.List;

@AllArgsConstructor @NoArgsConstructor @Builder
@Getter @Setter
public class PlanDetailsDto {
    private List<PlanItemDto> itemDtos;
}
