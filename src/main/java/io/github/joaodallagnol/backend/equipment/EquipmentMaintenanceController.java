package io.github.joaodallagnol.backend.equipment;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/equipment-maintenance")
public class EquipmentMaintenanceController {
    private final EquipmentMaintenanceService service;

    public EquipmentMaintenanceController(EquipmentMaintenanceService service) { this.service = service; }

    @GetMapping
    public List<MaintenanceRuleResponse> list() { return service.list(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaintenanceRuleResponse create(@Valid @RequestBody MaintenanceRuleRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{ruleId}")
    public MaintenanceRuleResponse update(@PathVariable UUID ruleId,
                                          @Valid @RequestBody MaintenanceRuleRequest request) {
        return service.update(ruleId, request);
    }

    @PostMapping("/{ruleId}/complete")
    public MaintenanceRuleResponse complete(@PathVariable UUID ruleId) { return service.markMaintained(ruleId); }

    @DeleteMapping("/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID ruleId) { service.delete(ruleId); }
}
