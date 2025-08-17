package pard.server.com.nadri.plan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import pard.server.com.nadri.plan.dto.res.PlaceItemDto;

@Entity
@DiscriminatorValue("MOVE")
@NoArgsConstructor @SuperBuilder
@Getter
public class MoveItem extends PlanItem{
    public static MoveItem from(PlaceItemDto itemDto){
        return MoveItem.builder()
                .title(itemDto.getTitle())
                .duration(itemDto.getDuration())
                .cost(itemDto.getCost())
                .build();
    }
}
