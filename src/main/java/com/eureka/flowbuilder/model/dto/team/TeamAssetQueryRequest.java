package com.eureka.flowbuilder.model.dto.team;

import com.eureka.flowbuilder.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TeamAssetQueryRequest extends PageRequest {
    private Long teamId;
    private String searchText;
    private String tag;
}
