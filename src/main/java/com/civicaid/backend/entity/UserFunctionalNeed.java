package com.civicaid.backend.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "user_functional_needs")
public class UserFunctionalNeed {

    @EmbeddedId
    private UserFunctionalNeedId id;

    @NotNull
    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_functional_needs_user"))
    private User user;

    @NotNull
    @MapsId("functionalNeedId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "functional_need_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_functional_needs_functional_need"))
    private FunctionalNeed functionalNeed;

    protected UserFunctionalNeed() {
    }

    public UserFunctionalNeed(User user, FunctionalNeed functionalNeed) {
        this.user = user;
        this.functionalNeed = functionalNeed;
        this.id = new UserFunctionalNeedId(user.getId(), functionalNeed.getId());
    }

    public UserFunctionalNeedId getId() { return id; }
    public User getUser() { return user; }
    public FunctionalNeed getFunctionalNeed() { return functionalNeed; }
}
