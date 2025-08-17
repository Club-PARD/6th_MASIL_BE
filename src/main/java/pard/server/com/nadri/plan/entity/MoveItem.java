package pard.server.com.nadri.plan.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import pard.server.com.nadri.plan.dto.resChat.ItemDto;

@Entity
@DiscriminatorValue("MOVE")
@AllArgsConstructor @NoArgsConstructor @SuperBuilder
@Getter
public class MoveItem extends PlanItem{
    public static MoveItem from(ItemDto itemDto){
        return MoveItem.builder()
                .title(itemDto.getTitle())
                .duration(itemDto.getDuration())
                .cost(itemDto.getCost())
                .build();
    }
}
