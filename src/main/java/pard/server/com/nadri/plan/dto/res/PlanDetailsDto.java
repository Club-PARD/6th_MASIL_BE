package pard.server.com.nadri.plan.dto.res;

import lombok.*;
import pard.server.com.nadri.openai.dto.PlanItemDto;

import java.util.List;

@AllArgsConstructor @NoArgsConstructor @Builder
@Getter @Setter
public class PlanDetailsDto {
    private List<PlanItemDto> itemDtos;
}
