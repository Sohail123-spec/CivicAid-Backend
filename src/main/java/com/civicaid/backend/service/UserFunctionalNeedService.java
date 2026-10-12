package com.civicaid.backend.service;

import com.civicaid.backend.dto.FunctionalNeedResponse;
import com.civicaid.backend.dto.FunctionalNeedSelectionRequest;
import com.civicaid.backend.entity.FunctionalNeed;
import com.civicaid.backend.entity.User;
import com.civicaid.backend.entity.UserFunctionalNeed;
import com.civicaid.backend.repository.FunctionalNeedRepository;
import com.civicaid.backend.repository.UserFunctionalNeedRepository;
import com.civicaid.backend.security.AuthenticatedUser;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class UserFunctionalNeedService {

    private final UserFunctionalNeedRepository userFunctionalNeedRepository;
    private final FunctionalNeedRepository functionalNeedRepository;

    public UserFunctionalNeedService(
            UserFunctionalNeedRepository userFunctionalNeedRepository,
            FunctionalNeedRepository functionalNeedRepository) {

        this.userFunctionalNeedRepository = userFunctionalNeedRepository;
        this.functionalNeedRepository = functionalNeedRepository;
    }

    @Transactional
    public List<FunctionalNeedResponse> getMyFunctionalNeeds(
            AuthenticatedUser principal) {

        User user = principal.getUser();

        return userFunctionalNeedRepository
                .findByUserId(user.getId())
                .stream()
                .map(UserFunctionalNeed::getFunctionalNeed)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<FunctionalNeedResponse> replaceMyFunctionalNeeds(
            AuthenticatedUser principal,
            FunctionalNeedSelectionRequest request) {

        User user = principal.getUser();

        Set<String> requestedCodes = request.functionalNeedCodes()
                .stream()
                .map(String::trim)
                .map(code -> code.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());

        List<FunctionalNeed> selectedNeeds =
                functionalNeedRepository.findAllByCodeInIgnoreCase(
                        requestedCodes.stream().toList());

        if (selectedNeeds.size() != requestedCodes.size()) {
            throw new IllegalArgumentException(
                    "One or more functional need codes are invalid");
        }

        boolean containsInvalidNeed = selectedNeeds.stream()
                .anyMatch(need -> !need.isActive() || !need.isSelectable());

        if (containsInvalidNeed) {
            throw new IllegalArgumentException(
                    "One or more functional needs are not available for selection");
        }

        userFunctionalNeedRepository.deleteByUserId(user.getId());

        List<UserFunctionalNeed> selections = selectedNeeds.stream()
                .map(need -> new UserFunctionalNeed(user, need))
                .toList();

        userFunctionalNeedRepository.saveAll(selections);

        return selectedNeeds.stream()
                .map(this::toResponse)
                .toList();
    }

    private FunctionalNeedResponse toResponse(FunctionalNeed need) {
        UUID parentNeedId = need.getParentNeed() != null
                ? need.getParentNeed().getId()
                : null;

        return new FunctionalNeedResponse(
                need.getId(),
                need.getCode(),
                need.getLabel(),
                need.getDescription(),
                parentNeedId,
                need.isSelectable(),
                need.getDisplayOrder()
        );
    }
}
