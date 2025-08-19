package pard.server.com.nadri.plan.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import pard.server.com.nadri.openai.dto.MealItemDto;
import pard.server.com.nadri.openai.dto.MoveItemDto;
import pard.server.com.nadri.openai.dto.PlaceItemDto;
import pard.server.com.nadri.openai.dto.PlanItemDto;

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

    public static PlanItem from(PlanItemDto planItemDto){
        if(planItemDto instanceof MealItemDto mealItemDto){
            return MealItem.from(mealItemDto);
        }else if(planItemDto instanceof  MoveItemDto moveItemDto){
            return MoveItem.from(moveItemDto);
        }else if(planItemDto instanceof PlaceItemDto placeItemDto){
            return PlaceItem.from(placeItemDto);
        }else{
            return null;
        }
    }

    public void savePlan(Plan plan){
        this.plan = plan;
    }
}
