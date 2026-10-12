package com.smartinventory.inventory.repository;

import com.smartinventory.inventory.model.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {
    Optional<PermissionEntity> findByName(String name);
}
