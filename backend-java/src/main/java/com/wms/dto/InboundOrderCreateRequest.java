package com.wms.dto;

import com.wms.dto.InboundItemRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InboundOrderCreateRequest {

    @NotBlank(message = "供应商名称不能为空")
    private String supplierName;

    @NotEmpty(message = "入库明细不能为空")
    @Valid
    private List<InboundItemRequest> items;
}
