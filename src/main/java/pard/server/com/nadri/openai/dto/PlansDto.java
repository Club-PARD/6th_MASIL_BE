package pard.server.com.nadri.openai.dto;

import lombok.*;

import java.util.List;

@AllArgsConstructor @NoArgsConstructor @Builder
@Getter @Setter
public class PlansDto { // 가이드 세트 (3개)
    private List<PlanDto> planDtos;
}
