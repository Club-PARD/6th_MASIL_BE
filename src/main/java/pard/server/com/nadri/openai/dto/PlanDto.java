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
    private List<PlanItemDto> planItemDtos;
    private String endTime;
}
