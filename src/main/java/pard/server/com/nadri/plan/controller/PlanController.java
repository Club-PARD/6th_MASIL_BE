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
@RequestMapping("/api/v1/plans")
public class PlanController {
    private final PlanService planService;

    /**
     * Create a new plan
     */
    @PostMapping
    public ResponseEntity<ResponsePlansDto> createPlan(@Valid @RequestBody CreatePlanDto createPlanDto) {
        try {
            ResponsePlansDto response = planService.createPlan(createPlanDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Get all plans by plans ID
     */
    @GetMapping("/{plansId}")
    public ResponseEntity<ResponsePlansDto> getPlans(@PathVariable Long plansId) {
        try {
            ResponsePlansDto response = planService.getPlansDto(plansId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Get specific plan details
     */
    @GetMapping("/{plansId}/plan/{planId}")
    public ResponseEntity<PlanDetailsDto> getPlanDetails(
            @PathVariable Long plansId, 
            @PathVariable Long planId) {
        try {
            PlanDetailsDto response = planService.getDetails(planId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }


    /**
     * Reload/regenerate plans (if this functionality is needed)
     */
    @PostMapping("/{plansId}/reload")
    public ResponseEntity<ResponsePlansDto> reloadPlans(@PathVariable Long plansId) {
        try {
            // Uncomment when service method is implemented
            // ResponsePlansDto response = planService.reloadPlans(plansId);
            // return ResponseEntity.ok(response);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
