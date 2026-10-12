package com.civicaid.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class UserFunctionalNeedId implements Serializable {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "functional_need_id")
    private UUID functionalNeedId;

    protected UserFunctionalNeedId() {
    }

    public UserFunctionalNeedId(UUID userId, UUID functionalNeedId) {
        this.userId = userId;
        this.functionalNeedId = functionalNeedId;
    }

    public UUID getUserId() { return userId; }
    public UUID getFunctionalNeedId() { return functionalNeedId; }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserFunctionalNeedId that)) {
            return false;
        }
        return Objects.equals(userId, that.userId)
                && Objects.equals(functionalNeedId, that.functionalNeedId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, functionalNeedId);
    }
}
