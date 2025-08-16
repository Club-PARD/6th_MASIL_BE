package pard.server.com.nadri.plan.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import pard.server.com.nadri.plan.dto.ResponseMoveDto;
import pard.server.com.nadri.plan.dto.ResponsePlaceDto;

@Entity
@DiscriminatorValue("PLACE")
@AllArgsConstructor @NoArgsConstructor @SuperBuilder
@Getter
public class PlaceItem extends PlanItem{
    private String description;
    private String linkUrl;

    public static PlaceItem from(ResponsePlaceDto responsePlaceDto){
        return PlaceItem.builder()
                .title(responsePlaceDto.getTitle())
                .duration(responsePlaceDto.getDuration())
                .cost(responsePlaceDto.getCost())
                .description(responsePlaceDto.getDescription())
                .linkUrl(responsePlaceDto.getLinkUrl())
                .build();
    }
}
