package com.kylin.fast.openai.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Created by ZengShilin on 2023/3/10 10:42 AM
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ModerationsRequest {

    /**
     * The input text to classify
     */
    String input;

    /**
     * wo content moderations models are available: text-moderation-stable and text-moderation-latest.
     * <p>
     * The default is text-moderation-latest which will be automatically upgraded over time.
     * This ensures you are always using our most accurate model.
     * If you use text-moderation-stable, we will provide advanced notice before updating the model.
     * Accuracy of text-moderation-stable may be slightly lower than for text-moderation-latest.
     */
    String model;
}
