package pard.server.com.nadri.plan.dto.res;

import lombok.*;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor @NoArgsConstructor @SuperBuilder
@Getter @Setter
public class PlaceItemDto extends PlanItemDto{
    private String description;
    private String linkUrl;
}
