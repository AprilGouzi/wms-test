package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.dto.InboundItemRequest;
import com.wms.dto.InboundOrderCreateRequest;
import com.wms.dto.InventoryResponse;
import com.wms.entity.Inventory;
import com.wms.entity.Location;
import com.wms.entity.Product;
import com.wms.entity.Warehouse;
import com.wms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InventoryServiceTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    private Product testProduct;
    private Warehouse testWarehouse;
    private Location testLocation;

    @BeforeEach
    void setUp() {
        // 创建测试仓库
        testWarehouse = warehouseRepository.save(Warehouse.builder()
                .code("WH-001")
                .name("测试仓库")
                .build());

        // 创建测试库位
        testLocation = locationRepository.save(Location.builder()
                .code("A-01-01")
                .warehouseId(testWarehouse.getId())
                .build());

        // 创建测试商品
        testProduct = productRepository.save(Product.builder()
                .name("测试商品")
                .sku("TEST-SKU-001")
                .build());
    }

    @Test
    void createInboundOrder_shouldCreateOrderAndUpdateInventory() {
        // 准备入库请求
        InboundItemRequest item = InboundItemRequest.builder()
                .productId(testProduct.getId())
                .quantity(100)
                .locationCode(testLocation.getCode())
                .build();

        InboundOrderCreateRequest request = InboundOrderCreateRequest.builder()
                .supplierName("测试供应商")
                .items(List.of(item))
                .build();

        // 执行
        Object result = inventoryService.createInboundOrder(request);

        // 验证
        assertNotNull(result);

        // 验证库存已更新
        Inventory inventory = inventoryRepository.findByProductIdAndLocationCode(
                testProduct.getId(), testLocation.getCode()).orElse(null);
        assertNotNull(inventory);
        assertEquals(100, inventory.getQuantity());
    }

    @Test
    void createInboundOrder_withMultipleItems_shouldUpdateAllInventories() {
        // 创建第二个库位
        Location location2 = locationRepository.save(Location.builder()
                .code("B-02-02")
                .warehouseId(testWarehouse.getId())
                .build());

        // 创建第二个商品
        Product product2 = productRepository.save(Product.builder()
                .name("测试商品2")
                .sku("TEST-SKU-002")
                .build());

        // 准备多行明细
        InboundItemRequest item1 = InboundItemRequest.builder()
                .productId(testProduct.getId())
                .quantity(50)
                .locationCode(testLocation.getCode())
                .build();

        InboundItemRequest item2 = InboundItemRequest.builder()
                .productId(product2.getId())
                .quantity(30)
                .locationCode(location2.getCode())
                .build();

        InboundOrderCreateRequest request = InboundOrderCreateRequest.builder()
                .supplierName("测试供应商")
                .items(List.of(item1, item2))
                .build();

        // 执行
        inventoryService.createInboundOrder(request);

        // 验证两个库位库存都已更新
        Inventory inv1 = inventoryRepository.findByProductIdAndLocationCode(
                testProduct.getId(), testLocation.getCode()).orElse(null);
        assertNotNull(inv1);
        assertEquals(50, inv1.getQuantity());

        Inventory inv2 = inventoryRepository.findByProductIdAndLocationCode(
                product2.getId(), location2.getCode()).orElse(null);
        assertNotNull(inv2);
        assertEquals(30, inv2.getQuantity());
    }

    @Test
    void createInboundOrder_withNonExistentProduct_shouldThrowException() {
        InboundItemRequest item = InboundItemRequest.builder()
                .productId(99999L) // 不存在的商品
                .quantity(10)
                .locationCode(testLocation.getCode())
                .build();

        InboundOrderCreateRequest request = InboundOrderCreateRequest.builder()
                .supplierName("测试供应商")
                .items(List.of(item))
                .build();

        // 验证抛出业务异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> inventoryService.createInboundOrder(request));
        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains("商品不存在"));
    }

    @Test
    void createInboundOrder_withNonExistentLocation_shouldThrowException() {
        InboundItemRequest item = InboundItemRequest.builder()
                .productId(testProduct.getId())
                .quantity(10)
                .locationCode("NON-EXISTENT") // 不存在的库位
                .build();

        InboundOrderCreateRequest request = InboundOrderCreateRequest.builder()
                .supplierName("测试供应商")
                .items(List.of(item))
                .build();

        // 验证抛出业务异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> inventoryService.createInboundOrder(request));
        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains("库位不存在"));
    }

    @Test
    void createInboundOrder_accumulativeInventory_shouldAddToExisting() {
        // 先创建初始库存
        inventoryRepository.save(Inventory.builder()
                .productId(testProduct.getId())
                .locationCode(testLocation.getCode())
                .quantity(100)
                .build());

        // 再入库 50
        InboundItemRequest item = InboundItemRequest.builder()
                .productId(testProduct.getId())
                .quantity(50)
                .locationCode(testLocation.getCode())
                .build();

        InboundOrderCreateRequest request = InboundOrderCreateRequest.builder()
                .supplierName("测试供应商")
                .items(List.of(item))
                .build();

        inventoryService.createInboundOrder(request);

        // 验证库存累加为 150
        Inventory inventory = inventoryRepository.findByProductIdAndLocationCode(
                testProduct.getId(), testLocation.getCode()).orElse(null);
        assertNotNull(inventory);
        assertEquals(150, inventory.getQuantity());
    }

    @Test
    void queryInventory_shouldReturnPagedResults() {
        // 创建库存
        inventoryRepository.save(Inventory.builder()
                .productId(testProduct.getId())
                .locationCode(testLocation.getCode())
                .quantity(100)
                .build());

        // 查询
        List<InventoryResponse> results = inventoryService.queryInventory(null, null, 1, 10);
        long total = inventoryService.countInventory(null, null);

        // 验证
        assertFalse(results.isEmpty());
        assertTrue(total > 0);
    }

    @Test
    void queryInventory_withKeyword_shouldFilterResults() {
        // 创建库存
        inventoryRepository.save(Inventory.builder()
                .productId(testProduct.getId())
                .locationCode(testLocation.getCode())
                .quantity(100)
                .build());

        // 按商品名称搜索
        List<InventoryResponse> results = inventoryService.queryInventory("测试商品", null, 1, 10);
        long total = inventoryService.countInventory("测试商品", null);

        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(r -> r.getProductName().contains("测试商品")));
    }

    @Test
    void queryInventory_withWarehouseFilter_shouldFilterByWarehouse() {
        // 创建库存
        inventoryRepository.save(Inventory.builder()
                .productId(testProduct.getId())
                .locationCode(testLocation.getCode())
                .quantity(100)
                .build());

        // 按仓库筛选
        List<InventoryResponse> results = inventoryService.queryInventory(null, testWarehouse.getId(), 1, 10);
        long total = inventoryService.countInventory(null, testWarehouse.getId());

        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(r -> r.getWarehouseName().equals(testWarehouse.getName())));
    }
}
