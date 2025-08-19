package pard.server.com.nadri.plan.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import pard.server.com.nadri.openai.dto.PlaceItemDto;

@Entity
@DiscriminatorValue("PLACE")
@AllArgsConstructor @NoArgsConstructor @SuperBuilder
@Getter
public class PlaceItem extends PlanItem{
    private String description;
    private String linkUrl;
    private String placeName;

    public static PlaceItem from(PlaceItemDto itemDto){
        return PlaceItem.builder()
                .title(itemDto.getTitle())
                .duration(itemDto.getDuration())
                .cost(itemDto.getCost())
                .orderNum(itemDto.getOrderNum())
                .description(itemDto.getDescription())
                .placeName(itemDto.getPlaceName())
                .linkUrl(itemDto.getLinkUrl())
                .build();
    }
}
