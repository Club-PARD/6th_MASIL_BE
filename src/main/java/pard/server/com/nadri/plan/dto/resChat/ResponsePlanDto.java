package pard.server.com.nadri.plan.dto.resChat;

import lombok.*;

import java.util.List;

@AllArgsConstructor @NoArgsConstructor @Builder
@Getter @Setter
public class ResponsePlanDto {
    private List<ItemDto> itemDtos;
}
