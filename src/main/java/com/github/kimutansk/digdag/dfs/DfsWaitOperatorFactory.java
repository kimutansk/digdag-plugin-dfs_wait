package com.github.kimutansk.digdag.dfs;

import io.digdag.spi.Operator;
import io.digdag.spi.OperatorContext;
import io.digdag.spi.OperatorFactory;

public class DfsWaitOperatorFactory implements OperatorFactory {
    @Override
    public String getType() {
        return "dfs_wait";
    }

    @Override
    public Operator newOperator(OperatorContext operatorContext) {
        return new DfsWaitOperator(operatorContext);
    }
}
