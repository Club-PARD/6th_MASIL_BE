package pard.server.com.nadri.openai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalTime;

@AllArgsConstructor @NoArgsConstructor @SuperBuilder
@Getter @Setter
public class PlaceItemDto extends PlanItemDto {
    private String description;

    @JsonProperty("link_url")
    private String linkUrl;

    @JsonProperty("place_name")
    private String placeName;

    public static PlaceItemDto of(String title, int orderNum, Integer cost, String duration, LocalTime startTime, String description, String linkUrl, String placeName){
        return PlaceItemDto.builder()
                .title(title)
                .orderNum(orderNum)
                .cost(cost)
                .duration(duration)
                .startTime(startTime)
                .placeName(placeName)
                .description(description)
                .linkUrl(linkUrl)
                .build();
    }
}
