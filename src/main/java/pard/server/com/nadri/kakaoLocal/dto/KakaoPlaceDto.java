package pard.server.com.nadri.kakaoLocal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@AllArgsConstructor @NoArgsConstructor @Builder
@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)

public class KakaoPlaceDto {
    @JsonProperty("documents")
    private List<PlaceDto> documents;
}
