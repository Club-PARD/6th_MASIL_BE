package pard.server.com.nadri.plan.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import pard.server.com.nadri.plan.dto.resChat.ItemDto;

@Entity
@DiscriminatorValue("MEAL")
@AllArgsConstructor @NoArgsConstructor @SuperBuilder
@Getter
public class MealItem extends PlanItem {
    public static MealItem from(ItemDto itemDto) {
        return MealItem.builder()
                .title(itemDto.getTitle())
                .startTime(itemDto.getStartTime())
                .duration("60분 소요") // 고정
                .build();
    }
}
