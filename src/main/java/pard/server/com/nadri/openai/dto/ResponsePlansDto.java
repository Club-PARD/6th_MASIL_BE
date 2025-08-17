package pard.server.com.nadri.openai.dto;

import lombok.*;

import java.util.List;

@AllArgsConstructor @NoArgsConstructor @Builder
@Getter @Setter
public class ResponsePlansDto {
    List<ResponsePlanDto> responsePlanDtos;
}
