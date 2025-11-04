package pard.server.com.nadri.plan.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pard.server.com.nadri.plan.dto.req.CreatePlanDto;
import pard.server.com.nadri.plan.dto.res.PlanDetailsDto;
import pard.server.com.nadri.plan.dto.res.ResponsePlansDto;
import pard.server.com.nadri.plan.service.PlanService;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/plans")
public class PlanController {
    private final PlanService planService;

    @PostMapping
    public ResponseEntity<ResponsePlansDto> createPlan(@RequestBody CreatePlanDto createPlanDto) {
        try {
            ResponsePlansDto response = planService.createPlan(createPlanDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            System.out.println("조졌습니다" + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{plansId}")
    public ResponseEntity<ResponsePlansDto> getPlans(@PathVariable Long plansId) {
        try {
            ResponsePlansDto response = planService.getPlansDto(plansId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/planDetails/{planId}")
    public ResponseEntity<PlanDetailsDto> getPlanDetails(
            @PathVariable Long planId) {
        try {
            PlanDetailsDto response = planService.getDetails(planId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
