package com.business.erp.inventory.repository;

import com.business.erp.inventory.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findByStatus(Material.MaterialStatus status);

    List<Material> findByMaterialTypeAndStatus(Material.MaterialType type, Material.MaterialStatus status);
}
