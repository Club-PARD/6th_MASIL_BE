package pard.server.com.nadri.plan.dto.res;

import lombok.*;

import java.util.List;

@AllArgsConstructor @NoArgsConstructor @Builder
@Getter @Setter
public class ResponsePlansDto { // 프론트에 보내줘야되는 PLans DTO
    private List<ResponsePlanDto> responsePlanDtos;
    private Long plansId;
}
