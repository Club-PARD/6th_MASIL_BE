package pard.server.com.nadri.plan.dto;

import lombok.*;

import java.time.LocalTime;

@AllArgsConstructor @NoArgsConstructor @Builder
@Getter @Setter
public class ResponsePlaceDto {
    private String title; // 제목
    private LocalTime startTime; // 시작 시간
    protected String duration; // 소요 시간
    private int cost; // 비용
    private String description;
    private String linkUrl;
}
