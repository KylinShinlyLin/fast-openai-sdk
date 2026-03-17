package com.kylin.fast.openai.result;

import com.kylin.fast.openai.result.dto.ModerationsDetail;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Created by ZengShilin on 2023/3/10 10:42 AM
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class ModerationsResult {

    String id;

    String model;

    List<ModerationsDetail> results;

}
