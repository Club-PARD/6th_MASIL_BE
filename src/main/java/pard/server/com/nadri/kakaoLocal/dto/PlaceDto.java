package pard.server.com.nadri.kakaoLocal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
        "category_group_name",
        "place_name",
        "place_url",
        "road_address_name",
        "x",
        "y"
})
public class PlaceDto {

    @JsonProperty("category_group_name")
    private final String categoryGroupName;

    @JsonProperty("address_name")
    private final String addressName;

    @JsonProperty("place_name")
    private final String placeName;

    @JsonProperty("place_url")
    private final String placeUrl;

    @JsonProperty("road_address_name")
    private final String roadAddressName;

    @JsonProperty("x")
    private final String x;

    @JsonProperty("y")
    private final String y;
}
