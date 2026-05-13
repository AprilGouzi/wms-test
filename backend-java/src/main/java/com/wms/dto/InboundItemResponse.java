package com.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InboundItemResponse {
    private Long productId;
    private String productName;
    private Integer quantity;
    private String locationCode;
}
