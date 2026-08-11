package com.eureka.flowbuilder.model.dto.app;

import lombok.Data;

@Data
public class ReplaceImageRequest {
    private Long appId;
    private Long assetId;
    private String oldUrl;
}
