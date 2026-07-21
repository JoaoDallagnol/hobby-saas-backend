package io.github.joaodallagnol.backend.subscription;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/plan")
public class SubscriptionController {

    private final EntitlementService entitlementService;

    public SubscriptionController(EntitlementService entitlementService) {
        this.entitlementService = entitlementService;
    }

    @GetMapping
    public CurrentPlanResponse currentPlan() {
        return entitlementService.currentPlan();
    }
}
