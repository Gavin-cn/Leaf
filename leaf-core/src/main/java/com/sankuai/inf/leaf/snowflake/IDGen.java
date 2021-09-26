package com.sankuai.inf.leaf.snowflake;

import com.sankuai.inf.leaf.common.Result;

public interface IDGen {
    Result get();
    boolean init();
}
