package pard.server.com.nadri.openai.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalTime;

@SuperBuilder
@Getter @Setter
public class MealItemDto extends PlanItemDto {
    public static MealItemDto of(String title, int orderNum, String duration, LocalTime startTime){
        return MealItemDto.builder()
                .title(title)
                .orderNum(orderNum)
                .duration(duration)
                .startTime(startTime)
                .build();
    }
}
