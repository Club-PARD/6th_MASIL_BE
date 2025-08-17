package pard.server.com.nadri.plan.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import pard.server.com.nadri.plan.dto.res.PlaceItemDto;

@Entity
@DiscriminatorValue("PLACE")
@AllArgsConstructor @NoArgsConstructor @SuperBuilder
@Getter
public class PlaceItem extends PlanItem{
    private String description;
    private String linkUrl;

    public static PlaceItem from(PlaceItemDto itemDto){
        return PlaceItem.builder()
                .title(itemDto.getTitle())
                .duration(itemDto.getDuration())
                .cost(itemDto.getCost())
                .description(itemDto.getDescription())
                .linkUrl(itemDto.getLinkUrl())
                .build();
    }
}
