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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EquipmentResponse createEquipment(@Valid @RequestBody CreateEquipmentRequest request) {
        return equipmentService.createEquipment(request);
    }

    @GetMapping
    public List<EquipmentResponse> listEquipment(@RequestParam(required = false) UUID hobbyId) {
        return equipmentService.listEquipment(hobbyId);
    }

    @PatchMapping("/{equipmentId}")
    public EquipmentResponse updateEquipment(
            @PathVariable UUID equipmentId,
            @Valid @RequestBody UpdateEquipmentRequest request
    ) {
        return equipmentService.updateEquipment(equipmentId, request);
    }

    @DeleteMapping("/{equipmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEquipment(@PathVariable UUID equipmentId) {
        equipmentService.deleteEquipment(equipmentId);
    }
}
