package com.civicaid.backend.service;

import com.civicaid.backend.dto.FunctionalNeedResponse;
import com.civicaid.backend.entity.FunctionalNeed;
import com.civicaid.backend.repository.FunctionalNeedRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FunctionalNeedService {

    private final FunctionalNeedRepository functionalNeedRepository;

    public FunctionalNeedService(FunctionalNeedRepository functionalNeedRepository) {
        this.functionalNeedRepository = functionalNeedRepository;
    }

    @Transactional(readOnly = true)
    public List<FunctionalNeedResponse> getAvailableNeeds() {
        return functionalNeedRepository
                .findByActiveTrueAndSelectableTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private FunctionalNeedResponse toResponse(FunctionalNeed need) {
        return new FunctionalNeedResponse(
                need.getId(),
                need.getCode(),
                need.getLabel(),
                need.getDescription(),
                need.getParentNeed() != null
                        ? need.getParentNeed().getId()
                        : null,
                need.isSelectable(),
                need.getDisplayOrder()
        );
    }
}