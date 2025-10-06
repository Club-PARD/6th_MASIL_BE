package pard.server.com.nadri.openai.dto;// 부모
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalTime;
import lombok.*;

@Getter @Setter @NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // 모르는 필드 들어와도 안전
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PlanItemDto.MoveItemDto.class,  name = "MOVE"),
        @JsonSubTypes.Type(value = PlanItemDto.MealItemDto.class,  name = "MEAL"),
        @JsonSubTypes.Type(value = PlanItemDto.PlaceItemDto.class, name = "PLACE")
})
public abstract class PlanItemDto {
    private String type; // MOVE | MEAL | PLACE
    private String title;

    @JsonProperty("order_num")
    private Integer orderNum;

    // 모델은 "60" 같은 문자열로 보냄
    private String duration;

    @JsonProperty("start_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime startTime;

    private Integer cost;

    // ----------------- 서브 클래스들 -----------------

    @Getter @Setter @NoArgsConstructor
    public static class MealItemDto extends PlanItemDto {
        public static MealItemDto of(String title, int orderNum, String duration, LocalTime startTime) {
            MealItemDto m = new MealItemDto();
            m.setType("MEAL");
            m.setTitle(title);
            m.setOrderNum(orderNum);
            m.setDuration(duration);
            m.setStartTime(startTime);
            return m;
        }
    }

    @Getter @Setter @NoArgsConstructor
    public static class MoveItemDto extends PlanItemDto {
        public static MoveItemDto of(String title, int orderNum, Integer cost, String duration, LocalTime startTime) {
            MoveItemDto mv = new MoveItemDto();
            mv.setType("MOVE");
            mv.setTitle(title);
            mv.setOrderNum(orderNum);
            mv.setCost(cost);
            mv.setDuration(duration);
            mv.setStartTime(startTime);
            return mv;
        }
    }

    @Getter @Setter @NoArgsConstructor
    public static class PlaceItemDto extends PlanItemDto {
        private String description;

        @JsonProperty("link_url")
        private String linkUrl;

        @JsonProperty("place_name")
        private String placeName;

        public static PlaceItemDto of(String title, int orderNum, Integer cost, String duration, LocalTime startTime,
                                      String description, String linkUrl, String placeName) {
            PlaceItemDto p = new PlaceItemDto();
            p.setType("PLACE");
            p.setTitle(title);
            p.setOrderNum(orderNum);
            p.setCost(cost);
            p.setDuration(duration);
            p.setStartTime(startTime);
            p.setDescription(description);
            p.setLinkUrl(linkUrl);
            p.setPlaceName(placeName);
            return p;
        }
    }
}
