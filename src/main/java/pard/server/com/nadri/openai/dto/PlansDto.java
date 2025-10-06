package pard.server.com.nadri.openai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@AllArgsConstructor @NoArgsConstructor @Builder
@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlansDto { // 가이드 세트 (3개)
    private List<PlanDto> planDtos;
}
