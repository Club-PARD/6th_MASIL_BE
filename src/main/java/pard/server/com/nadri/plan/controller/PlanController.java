package pard.server.com.nadri.plan.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pard.server.com.nadri.plan.dto.req.CreatePlanDto;
import pard.server.com.nadri.plan.service.PlanService;

@RestController
@RequiredArgsConstructor
@RequestMapping("plan")
public class PlanController {
    private final PlanService planService;

    @PostMapping("")
    public void createPlan(@RequestBody CreatePlanDto createPlanDto){
        planService.createPlan(createPlanDto);
    }
}
