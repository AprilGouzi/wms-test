package com.wms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InboundItemRequest {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Min(value = 1, message = "数量必须大于0")
    private Integer quantity;

    @NotBlank(message = "库位编码不能为空")
    private String locationCode;
}
