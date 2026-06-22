package com.dadian.module.outing.model;

import lombok.Data;

@Data
public class MemoryGenerateRequest {
    private String outingId;
    private String style = "wangjiawei";
    private String visibility = "private";
}
