package com.kylin.fast.openai.strategy;

import java.util.List;

/**
 * Created by ZengShilin on 2023/5/23 6:05 PM
 */
public interface KeyStrategyFunction {

    /**
     * 路由策略
     *
     * @return
     */
    String routing();

    /**
     * 请求结果回调
     *
     * @param success 请求成功/失败
     * @param key     key
     * @return
     */
    void resultCallBack(boolean success, String key);

    /**
     * 获取当前所有key
     * @return
     */
    List<String> allTokens();

}
