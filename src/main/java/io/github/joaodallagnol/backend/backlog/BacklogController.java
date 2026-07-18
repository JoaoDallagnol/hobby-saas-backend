package io.github.joaodallagnol.backend.backlog;

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
@RequestMapping("/api/me/backlog-items")
public class BacklogController {

    private final BacklogService backlogService;

    public BacklogController(BacklogService backlogService) {
        this.backlogService = backlogService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BacklogItemResponse createItem(@Valid @RequestBody CreateBacklogItemRequest request) {
        return backlogService.createItem(request);
    }

    @GetMapping
    public List<BacklogItemResponse> listItems(@RequestParam(required = false) UUID hobbyId) {
        return backlogService.listItems(hobbyId);
    }

    @PatchMapping("/{itemId}")
    public BacklogItemResponse updateItem(
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateBacklogItemRequest request
    ) {
        return backlogService.updateItem(itemId, request);
    }

    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable UUID itemId) {
        backlogService.deleteItem(itemId);
    }
}
