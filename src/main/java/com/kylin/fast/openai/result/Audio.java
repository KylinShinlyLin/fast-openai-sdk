package com.kylin.fast.openai.result;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;
import java.util.Objects;

@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Audio {

    String id;

    /**
     * base64 数据
     */
    String data;

    Long expires_at;

    /**
     * 对应的原文
     */
    String transcript;


    File file;

    /**
     * 创建一个临时文件返回 对应音频文件(注意删除文件)
     * @return
     */
    @SneakyThrows
    public File audioFile() {
        if (Objects.nonNull(file)) {
            return file;
        }
        File wav = File.createTempFile("openai_audio", ".wav");
        file = wav;
        // 解码Base64字符串
        byte[] audioBytes = Base64.getDecoder().decode(data);

        // 保存为文件
        try (FileOutputStream fos = new FileOutputStream(wav.getAbsoluteFile())) {
            fos.write(audioBytes);
        }
        return wav;
    }
}
