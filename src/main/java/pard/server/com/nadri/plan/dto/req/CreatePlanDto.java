package pard.server.com.nadri.plan.dto.req;

import lombok.*;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Date;

@AllArgsConstructor @NoArgsConstructor @Builder
@Getter @Setter
public class CreatePlanDto {
    private String origin; // 출발지
    private int budget; // 예산
    private int headcount; // 인원수
    private String transportation; // 이동수단 (버스, 지하철, 도보중 택일)
    private String date; // "yyyy-MM-dd"
    private String timeTable; // "hh:mm~hh:mm"
    private String theme; // 여행 테마 (축제 문화, 원데이 클래스 체험, 자연 경관, 쇼핑중 택일)

    public String getDescription(String theme){
        return TravelTheme.fromLabel(theme).getDescription();
    }

    // timeTable 형태보고 나눠서 반환하기 위한 애들.
//    public String getStartTime(){
//
//    }
//    public String getEndTime(){
//
//    }
}
