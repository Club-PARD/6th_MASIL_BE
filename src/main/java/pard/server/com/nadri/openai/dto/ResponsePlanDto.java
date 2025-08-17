package pard.server.com.nadri.openai.dto;

import lombok.*;

import java.util.List;

@AllArgsConstructor @NoArgsConstructor @Builder
@Getter @Setter
public class ResponsePlanDto {
    private String order;
    private List<ItemDto> itemDtos;
}
