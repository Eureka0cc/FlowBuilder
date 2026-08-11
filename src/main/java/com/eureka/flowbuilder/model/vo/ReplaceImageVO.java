package com.eureka.flowbuilder.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReplaceImageVO {
    private int replacementCount;
    private List<String> affectedPaths = new ArrayList<>();
}
