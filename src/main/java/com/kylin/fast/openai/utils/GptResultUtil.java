package com.kylin.fast.openai.utils;

import com.kylin.fast.openai.request.dto.Message;
import com.kylin.fast.openai.result.ChatResult;
import com.kylin.fast.openai.result.dto.ChatChoice;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.stream.Collectors;

public class GptResultUtil {

    public static String response(ChatResult chatResult) {
        if (Objects.isNull(chatResult)) {
            return null;
        }

        return chatResult.getChoices()
                .stream()
                .map(ChatChoice::getMessage)
                .map(Message::getContent)
                .filter(e -> !"\n".equals(e))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining()).trim();
    }
}
