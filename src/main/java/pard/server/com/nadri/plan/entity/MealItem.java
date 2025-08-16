package pard.server.com.nadri.plan.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import pard.server.com.nadri.plan.dto.ResponseMealDto;

import java.time.LocalTime;

@Entity
@DiscriminatorValue("MEAL")
@AllArgsConstructor @NoArgsConstructor @SuperBuilder
@Getter
public class MealItem extends PlanItem {
    public static MealItem from(ResponseMealDto responseMealDto) {
        return MealItem.builder()
                .title(responseMealDto.getTitle())
                .startTime(responseMealDto.getStartTime())
                .duration("60분 소요") // 고정
                .build();
    }
}
