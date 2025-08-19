package pard.server.com.nadri.plan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import pard.server.com.nadri.openai.dto.MealItemDto;
import pard.server.com.nadri.openai.dto.PlanItemDto;

@Entity
@DiscriminatorValue("MEAL")
@NoArgsConstructor @SuperBuilder
@Getter
public class MealItem extends PlanItem {
    public static MealItem from(MealItemDto itemDto) {
        return MealItem.builder()
                .title(itemDto.getTitle())
                .startTime(itemDto.getStartTime())
                .duration("60분 소요") // 고정
                .orderNum(itemDto.getOrderNum())
                .build();
    }
}
