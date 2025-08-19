package pard.server.com.nadri.openai.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalTime;

@SuperBuilder
@Getter @Setter
public class MoveItemDto extends PlanItemDto {
    private boolean isTransport;
    public static MoveItemDto of(String title, int orderNum, Integer cost, String duration, LocalTime startTime){
        return MoveItemDto.builder()
                .title(title)
                .orderNum(orderNum)
                .cost(cost)
                .duration(duration)
                .startTime(startTime)
                .isTransport(true)
                .build();
    }
}
