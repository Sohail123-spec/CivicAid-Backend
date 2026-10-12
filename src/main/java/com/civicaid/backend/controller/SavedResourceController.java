package com.civicaid.backend.controller;

import com.civicaid.backend.dto.SavedResourceCreateRequest;
import com.civicaid.backend.dto.SavedResourceResponse;
import com.civicaid.backend.security.AuthenticatedUser;
import com.civicaid.backend.service.SavedResourceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/saved-resources")
public class SavedResourceController {

    private final SavedResourceService savedResourceService;

    public SavedResourceController(
            SavedResourceService savedResourceService) {
        this.savedResourceService = savedResourceService;
    }

    @GetMapping
    public List<SavedResourceResponse> getMySavedResources(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        return savedResourceService.getMySavedResources(
                authenticatedUser.getUsername());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SavedResourceResponse saveResource(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody SavedResourceCreateRequest request) {

        return savedResourceService.saveResource(
                request,
                authenticatedUser.getUsername());
    }

    @DeleteMapping("/{savedResourceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSavedResource(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID savedResourceId) {

        savedResourceService.deleteMySavedResource(
                savedResourceId,
                authenticatedUser.getUsername());
    }
}