package pard.server.com.nadri.plan.dto;

import lombok.*;

import java.time.LocalTime;

@AllArgsConstructor @NoArgsConstructor @Builder
@Getter @Setter
public class ResponseMealDto {
    private String title;
    private LocalTime startTime;
}
