package pard.server.com.nadri.plan.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import pard.server.com.nadri.plan.dto.ResponseMoveDto;

@Entity
@DiscriminatorValue("MOVE")
@AllArgsConstructor @NoArgsConstructor @SuperBuilder
@Getter
public class MoveItem extends PlanItem{
    public static MoveItem from(ResponseMoveDto responseMoveItem){
        return MoveItem.builder()
                .title(responseMoveItem.getTitle())
                .duration(responseMoveItem.getDuration())
                .cost(responseMoveItem.getCost())
                .build();
    }
}
