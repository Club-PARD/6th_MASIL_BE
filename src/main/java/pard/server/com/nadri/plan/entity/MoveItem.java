package pard.server.com.nadri.plan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import pard.server.com.nadri.openai.dto.MoveItemDto;
import pard.server.com.nadri.openai.dto.PlaceItemDto;
import pard.server.com.nadri.openai.dto.PlanItemDto;

@Entity
@DiscriminatorValue("MOVE")
@NoArgsConstructor @SuperBuilder
@Getter
public class MoveItem extends PlanItem{
    public static MoveItem from(MoveItemDto itemDto){
        return MoveItem.builder()
                .title(itemDto.getTitle())
                .duration(itemDto.getDuration())
                .cost(itemDto.getCost())
                .startTime(itemDto.getStartTime())
                .orderNum(itemDto.getOrderNum())
                .build();
    }
}
