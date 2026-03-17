package com.kylin.fast.openai.request.dto;

import com.alibaba.fastjson.JSONObject;
import com.kylin.fast.openai.constant.MessageType;

/**
 * Created by ZengShiLin on 2023/11/7 15:44
 *
 * @author ZengShiLin
 */
public interface BaseMessage {


    String roleType();

    MessageType messageType();


}
