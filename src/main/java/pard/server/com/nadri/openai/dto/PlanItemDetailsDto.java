package pard.server.com.nadri.openai.dto;// 부모
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalTime;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import lombok.*;

@Getter @Setter @NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // 모르는 필드 들어와도 안전
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PlanItemDetailsDto.MoveItemDetailsDto.class,  name = "MOVE"),
        @JsonSubTypes.Type(value = PlanItemDetailsDto.MealItemDetailsDto.class,  name = "MEAL"),
        @JsonSubTypes.Type(value = PlanItemDetailsDto.PlaceItemDetailsDto.class, name = "PLACE")
})
public abstract class PlanItemDetailsDto {
    private String type; // MOVE | MEAL | PLACE
    private String title;

    @JsonProperty("order_num")
    private Integer orderNum;

    // 모델은 "60" 같은 문자열로 보냄
    private String duration;

    @JsonProperty("start_time")
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime startTime;

    private Integer cost;

    // ----------------- 서브 클래스들 -----------------

    @Getter @Setter @NoArgsConstructor
    public static class MealItemDetailsDto extends PlanItemDetailsDto {
        public static MealItemDetailsDto of(String title, int orderNum, String duration, LocalTime startTime) {
            MealItemDetailsDto m = new MealItemDetailsDto();
            m.setType("MEAL");
            m.setTitle(title);
            m.setOrderNum(orderNum);
            m.setDuration(duration);
            m.setStartTime(startTime);
            return m;
        }
    }

    @Getter @Setter @NoArgsConstructor
    public static class MoveItemDetailsDto extends PlanItemDetailsDto {
        public static MoveItemDetailsDto of(String title, int orderNum, String duration, LocalTime startTime) {
            MoveItemDetailsDto mv = new MoveItemDetailsDto();
            mv.setType("MOVE");
            mv.setTitle(title);
            mv.setOrderNum(orderNum);
            mv.setDuration(duration);
            mv.setStartTime(startTime);
            return mv;
        }
    }

    @Getter @Setter @NoArgsConstructor
    public static class PlaceItemDetailsDto extends PlanItemDetailsDto {
        private String description;

        @JsonProperty("link_url")
        private String linkUrl;

        @JsonProperty("place_name")
        private String placeName;

        public static PlaceItemDetailsDto of(String title, int orderNum, Integer cost, String duration, LocalTime startTime,
                                             String description, String linkUrl, String placeName) {
            PlaceItemDetailsDto p = new PlaceItemDetailsDto();
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
