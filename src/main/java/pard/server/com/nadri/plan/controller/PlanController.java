package pard.server.com.nadri.plan.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pard.server.com.nadri.plan.dto.req.CreatePlanDto;
import pard.server.com.nadri.plan.dto.res.PlanDetailsDto;
import pard.server.com.nadri.plan.dto.res.ResponsePlansDto;
import pard.server.com.nadri.plan.service.PlanService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/plan")
public class PlanController {
    private final PlanService planService;

    @PostMapping("")
    public ResponsePlansDto createPlan(@RequestBody CreatePlanDto createPlanDto){
        return planService.createPlan(createPlanDto);
    }

    @PostMapping("/reload/{plansId}")
    public ResponsePlansDto reloadPlans(@PathVariable Long plansId){
        return planService.reloadPlans(plansId);
    }

    @GetMapping("/{planId}")
    public PlanDetailsDto getDetails(@PathVariable Long planId){
        return planService.getDetails(planId);
    }
}
