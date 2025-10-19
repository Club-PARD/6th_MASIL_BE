package pard.server.com.nadri.openai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor @NoArgsConstructor
@Builder @Getter @Setter
public class PlanDto {
    private String order;
    private List<PlanItemDetailsDto> planItemDetailsDtos;
}
