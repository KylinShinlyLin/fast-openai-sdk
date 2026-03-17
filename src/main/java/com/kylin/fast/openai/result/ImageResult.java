package com.kylin.fast.openai.result;

import com.alibaba.fastjson.JSONObject;
import com.kylin.fast.openai.result.dto.Image;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * Created by ZengShilin on 2023/3/2 5:38 PM
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
public class ImageResult {

    String created;

    List<Image> data;

    JSONObject usage;
}
