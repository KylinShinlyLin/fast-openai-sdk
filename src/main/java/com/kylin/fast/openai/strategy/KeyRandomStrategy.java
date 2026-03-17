package com.kylin.fast.openai.strategy;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class KeyRandomStrategy implements KeyStrategyFunction {

    private final List<String> keys;

    public KeyRandomStrategy(List<String> keys) {
        this.keys = keys;
    }

    @Override
    public String routing() {
        if (keys == null || keys.isEmpty()) {
            return null;
        }
        return keys.get(ThreadLocalRandom.current().nextInt(keys.size()));
    }

    @Override
    public void resultCallBack(boolean success, String key) {

    }

    @Override
    public List<String> allTokens() {
        return keys;
    }

}
