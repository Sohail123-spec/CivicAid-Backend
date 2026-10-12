package com.civicaid.backend.repository;

import com.civicaid.backend.entity.Role;
import com.civicaid.backend.entity.RoleName;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(RoleName name);
}
