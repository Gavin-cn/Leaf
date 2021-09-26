package com.sankuai.inf.leaf.snowflake.impl.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class IdWorkerHolder {
    private String serviceHost;
    private String serviceName;
    private LocalDateTime timestamp;
    private Integer workerId;
}
