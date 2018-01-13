package com.github.kimutansk.digdag.dfs;

import io.digdag.spi.OperatorFactory;
import io.digdag.spi.OperatorProvider;
import io.digdag.spi.Plugin;

import java.util.Arrays;
import java.util.List;

public class DfsWaitPlugin implements Plugin {
    @Override
    public <T> Class<? extends T> getServiceProvider(Class<T> type) {
        if (type == OperatorProvider.class) {
            return DfsWaitOperatorProvider.class.asSubclass(type);
        } else {
            return null;
        }
    }

    public static class DfsWaitOperatorProvider implements OperatorProvider {
        @Override
        public List<OperatorFactory> get() {
            return Arrays.asList(new DfsWaitOperatorFactory());
        }
    }
}
