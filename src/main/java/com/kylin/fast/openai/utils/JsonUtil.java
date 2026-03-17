package com.kylin.fast.openai.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ZengShilin on 2023/4/14 11:58 AM
 */
@Slf4j
public class JsonUtil {

    private static final Pattern regex = Pattern.compile("\\[.*?\\]");

    /**
     * 提取json
     *
     * @param input
     * @return
     */
    public static List<String> getJsonFromString(String input) {
        List<Character> stack = new ArrayList<>();
        List<String> jsons = new ArrayList<>();
        StringBuilder temp = new StringBuilder();
        for (char eachChar : input.toCharArray()) {
            if (stack.isEmpty() && eachChar == '{') {
                stack.add(eachChar);
                temp.append(eachChar);
            } else if (!stack.isEmpty()) {
                temp.append(eachChar);
                if (stack.get(stack.size() - 1).equals('{') && eachChar == '}') {
                    stack.remove(stack.size() - 1);
                    if (stack.isEmpty()) {
                        jsons.add(temp.toString());
                        temp = new StringBuilder();
                    }
                } else if (eachChar == '{' || eachChar == '}') {
                    stack.add(eachChar);
                }
            } else if (temp.length() > 0) {
                jsons.add(temp.toString());
                temp = new StringBuilder();
            }
        }
        return jsons;
    }

    public static String extractJsonArray(String input) {
        Matcher matcher = regex.matcher(input);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
}
