package pard.server.com.nadri.plan.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pard.server.com.nadri.openai.dto.PlanDto;

import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor @NoArgsConstructor @Builder
@Getter
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanItem> planItems;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "plans_id")
    private Plans plans;

    private String endTime;

    @Column(name = "sort_order")   // ✅ 예약어 회피
    private String order;

    public static Plan from(PlanDto planDto){
        List<PlanItem> planItems = new ArrayList<>();
        Plan plan = new Plan();

        planDto.getPlanItemDtos().forEach(planItemDto -> {
            PlanItem planItem = PlanItem.from(planItemDto);
            planItem.savePlan(plan);
            planItems.add(planItem);
        });

        plan.endTime = planDto.getEndTime();
        plan.order = planDto.getOrder();
        plan.planItems = planItems;

        return plan;
    }

    public void savePlans(Plans plans){
        this.plans = plans;
    }
}
