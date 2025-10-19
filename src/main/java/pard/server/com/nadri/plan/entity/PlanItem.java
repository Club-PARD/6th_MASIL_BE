package pard.server.com.nadri.plan.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import pard.server.com.nadri.openai.dto.PlanItemDetailsDto;

import java.time.LocalTime;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "item_type")
@AllArgsConstructor @NoArgsConstructor @SuperBuilder
@Getter
public abstract class PlanItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private Plan plan;

    private int orderNum; // 순서
    private String title; // 제목
    private LocalTime startTime; // 시작 시간
    private String duration; // 소요 시간
    private int cost; // 비용

    public static PlanItem from(PlanItemDetailsDto planItemDetailsDto){
        if(planItemDetailsDto instanceof PlanItemDetailsDto.MealItemDetailsDto mealItemDto){
            return MealItem.from(mealItemDto);
        }else if(planItemDetailsDto instanceof  PlanItemDetailsDto.MoveItemDetailsDto moveItemDto){
            return MoveItem.from(moveItemDto);
        }else if(planItemDetailsDto instanceof PlanItemDetailsDto.PlaceItemDetailsDto placeItemDto){
            return PlaceItem.from(placeItemDto);
        }else{
            return null;
        }
    }

    public void savePlan(Plan plan){
        this.plan = plan;
    }

    @Entity
    @DiscriminatorValue("MEAL")
    @NoArgsConstructor @SuperBuilder
    @Getter
    public static class MealItem extends PlanItem {
        public static MealItem from(PlanItemDetailsDto.MealItemDetailsDto itemDto) {
            return MealItem.builder()
                    .title(itemDto.getTitle())
                    .startTime(itemDto.getStartTime())
                    .duration("60") // 고정
                    .orderNum(itemDto.getOrderNum())
                    .build();
        }
    }

    @Entity
    @DiscriminatorValue("MOVE")
    @NoArgsConstructor @SuperBuilder
    @Getter
    public static class MoveItem extends PlanItem{
        public static MoveItem from(PlanItemDetailsDto.MoveItemDetailsDto itemDto){
            return MoveItem.builder()
                    .title(itemDto.getTitle())
                    .duration(itemDto.getDuration())
                    .startTime(itemDto.getStartTime())
                    .orderNum(itemDto.getOrderNum())
                    .build();
        }
    }

    @Entity
    @DiscriminatorValue("PLACE")
    @AllArgsConstructor @NoArgsConstructor @SuperBuilder
    @Getter
    public static class PlaceItem extends PlanItem{
        private String description;
        private String linkUrl;
        private String placeName;

        public static PlaceItem from(PlanItemDetailsDto.PlaceItemDetailsDto itemDto){
            return PlaceItem.builder()
                    .title(itemDto.getTitle())
                    .duration(itemDto.getDuration())
                    .startTime(itemDto.getStartTime())
                    .cost(itemDto.getCost())
                    .orderNum(itemDto.getOrderNum())
                    .description(itemDto.getDescription())
                    .placeName(itemDto.getPlaceName())
                    .linkUrl(itemDto.getLinkUrl())
                    .build();
        }
    }
}
