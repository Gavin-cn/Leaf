package com.sankuai.inf.leaf.snowflake;

public interface WorkerIdGenerator {
    boolean init() throws Exception;
    int getWorkerID();
}
