package pard.server.com.nadri.plan.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pard.server.com.nadri.openai.dto.ResponsePlansDto;
import pard.server.com.nadri.plan.dto.req.CreatePlanDto;
import pard.server.com.nadri.plan.dto.res.PlanDetailsDto;
import pard.server.com.nadri.plan.service.PlanService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/plan")
public class PlanController {
    private final PlanService planService;

    @PostMapping("")
    public void createPlan(@RequestBody CreatePlanDto createPlanDto){
        planService.createPlan(createPlanDto);
    }

    @GetMapping("")
    public PlanDetailsDto getDetails(@PathVariable Long planId){
        return planService.getDetails(planId);
    }
}
