package com.kylin.fast.openai.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class FunctionTrigger {

    String name;

    String id;

    StringBuilder arguments;

}
