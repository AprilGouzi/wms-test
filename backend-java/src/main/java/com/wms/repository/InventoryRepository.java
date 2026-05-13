package com.wms.repository;

import com.wms.entity.Inventory;
import com.wms.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductIdAndLocationCode(Long productId, String locationCode);

    long countByProductId(Long productId);

    @Query(value = "SELECT i.product_id, p.name, p.sku, i.location_code, w.name, i.quantity, i.updated_at " +
                   "FROM inventory i " +
                   "JOIN locations l ON i.location_code = l.code " +
                   "JOIN products p ON i.product_id = p.id " +
                   "JOIN warehouses w ON l.warehouse_id = w.id " +
                   "WHERE (:keyword IS NULL OR p.name LIKE CONCAT('%', :keyword, '%') OR p.sku LIKE CONCAT('%', :keyword, '%')) " +
                   "AND (:warehouseId IS NULL OR l.warehouse_id = :warehouseId) " +
                   "LIMIT :limit OFFSET :offset",
           nativeQuery = true)
    List<Object[]> searchInventoryRaw(@Param("keyword") String keyword,
                                      @Param("warehouseId") Long warehouseId,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);

    @Query(value = "SELECT COUNT(*) FROM inventory i " +
                   "JOIN locations l ON i.location_code = l.code " +
                   "JOIN products p ON i.product_id = p.id " +
                   "WHERE (:keyword IS NULL OR p.name LIKE CONCAT('%', :keyword, '%') OR p.sku LIKE CONCAT('%', :keyword, '%')) " +
                   "AND (:warehouseId IS NULL OR l.warehouse_id = :warehouseId)",
           nativeQuery = true)
    long countSearchInventory(@Param("keyword") String keyword,
                              @Param("warehouseId") Long warehouseId);

    // Optional<Location> findByCode(String code);
}
