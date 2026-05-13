package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.dto.InboundItemRequest;
import com.wms.dto.InboundOrderCreateRequest;
import com.wms.dto.InventoryResponse;
import com.wms.entity.InboundOrder;
import com.wms.entity.InboundOrderItem;
import com.wms.entity.Inventory;
import com.wms.entity.Location;
import com.wms.entity.Product;
import com.wms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InboundOrderRepository inboundOrderRepository;
    private final InboundOrderItemRepository inboundOrderItemRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;

    /**
     * 入库单创建
     */
    @Transactional
    public Object createInboundOrder(InboundOrderCreateRequest request) {
        // 1. 生成入库单号 IN-YYYYMMDD-XXX
        String orderNo = generateOrderNo();

        // 2. 创建入库单主表记录
        InboundOrder order = InboundOrder.builder()
                .orderNo(orderNo)
                .supplierName(request.getSupplierName())
                .status("COMPLETED")
                .build();
        order = inboundOrderRepository.save(order);

        // 3. 处理每个明细项
        for (var item : request.getItems()) {
            // 校验商品是否存在
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new BusinessException(404, "商品不存在: id=" + item.getProductId()));

            // 校验库位是否存在
            Location location = locationRepository.findByCode(item.getLocationCode())
                    .orElseThrow(() -> new BusinessException(404, "库位不存在: " + item.getLocationCode()));

            // 创建入库单明细
            InboundOrderItem orderItem = InboundOrderItem.builder()
                    .orderId(order.getId())
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .locationCode(item.getLocationCode())
                    .build();
            inboundOrderItemRepository.save(orderItem);

            // 4. 更新库存（累加）
            Inventory inventory = inventoryRepository.findByProductIdAndLocationCode(
                    item.getProductId(), item.getLocationCode()).orElse(null);

            if (inventory == null) {
                inventory = Inventory.builder()
                        .productId(item.getProductId())
                        .locationCode(item.getLocationCode())
                        .quantity(item.getQuantity())
                        .build();
            } else {
                inventory.setQuantity(inventory.getQuantity() + item.getQuantity());
            }
            inventoryRepository.save(inventory);
        }

        log.info("入库单创建成功: orderNo={}, supplier={}, items={}",
                orderNo, request.getSupplierName(), request.getItems().size());

        // 5. 返回创建结果（包含明细信息）
        return buildOrderResponse(order, request.getItems());
    }

    /**
     * 库存查询
     */
    public List<InventoryResponse> queryInventory(String keyword, Long warehouseId,
                                                 int page, int pageSize) {
        // 使用 JOIN 查询获取库存及其关联信息
        int offset = (page - 1) * pageSize;
        List<Object[]> results = inventoryRepository.searchInventoryRaw(keyword, warehouseId, offset, pageSize);
        return results.stream().map(this::toInventoryResponse).collect(Collectors.toList());
    }

    /**
     * 查询库存总数（用于分页）
     */
    public long countInventory(String keyword, Long warehouseId) {
        return inventoryRepository.countSearchInventory(keyword, warehouseId);
    }

    private String generateOrderNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long count = inboundOrderRepository.countCreatedToday(startOfDay) + 1;
        return String.format("IN-%s-%03d", dateStr, count);
    }

    private Object buildOrderResponse(InboundOrder order, List<InboundItemRequest> items) {
        // 构建响应数据
        var itemResponses = items.stream().map(item -> {
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            return java.util.Map.of(
                    "productId", item.getProductId(),
                    "productName", product != null ? product.getName() : "",
                    "quantity", item.getQuantity(),
                    "locationCode", item.getLocationCode()
            );
        }).collect(Collectors.toList());

        return java.util.Map.of(
                "id", order.getId(),
                "orderNo", order.getOrderNo(),
                "supplierName", order.getSupplierName(),
                "status", order.getStatus(),
                "items", itemResponses,
                "createdAt", order.getCreatedAt().toString()
        );
    }

    private InventoryResponse toInventoryResponse(Object[] row) {
        return InventoryResponse.builder()
                .productId((Long) row[0])
                .productName((String) row[1])
                .sku((String) row[2])
                .locationCode((String) row[3])
                .warehouseName((String) row[4])
                .quantity((Integer) row[5])
                .updatedAt(row[6] != null ? ((java.sql.Timestamp) row[6]).toLocalDateTime() : null)
                .build();
    }
}
