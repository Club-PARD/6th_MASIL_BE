package pard.server.com.nadri.plan.dto.res;

import lombok.*;

import java.util.List;

@AllArgsConstructor @NoArgsConstructor @Builder
@Getter @Setter
public class ResponsePlanDto { // 프론트에 보내줘야 되는 Plan DTO
    private Long planId;
    private String order;
    private String endTime;
    private List<ResponseItemDto> itemDtos;
}
