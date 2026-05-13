package com.wms.controller;

import com.wms.common.ApiResponse;
import com.wms.common.PageResult;
import com.wms.dto.InboundOrderCreateRequest;
import com.wms.dto.InventoryResponse;
import com.wms.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * 创建入库单
     */
    @PostMapping("/inbound-orders")
    public ApiResponse<?> createInboundOrder(@Valid @RequestBody InboundOrderCreateRequest request) {
        Object result = inventoryService.createInboundOrder(request);
        return new ApiResponse<>(201, "入库单创建成功", result);
    }

    /**
     * 库存查询
     */
    @GetMapping("/inventory")
    public ApiResponse<PageResult<InventoryResponse>> queryInventory(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        List<InventoryResponse> list = inventoryService.queryInventory(keyword, warehouseId, page, pageSize);
        long total = inventoryService.countInventory(keyword, warehouseId);
        PageResult<InventoryResponse> pageResult = new PageResult<>(list, total, page, pageSize);
        return ApiResponse.success(pageResult);
    }
}
