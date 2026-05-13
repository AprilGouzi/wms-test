package com.wms.repository;

import com.wms.entity.InboundOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface InboundOrderRepository extends JpaRepository<InboundOrder, Long> {

    // 用于生成入库单号时查询当天最大序号
    @Query("SELECT COUNT(i) FROM InboundOrder i WHERE i.createdAt >= :startOfDay")
    long countCreatedToday(@Param("startOfDay") LocalDateTime startOfDay);
}
