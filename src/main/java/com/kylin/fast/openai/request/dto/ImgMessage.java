package com.kylin.fast.openai.request.dto;

import com.google.common.collect.Lists;
import com.kylin.fast.openai.constant.MessageType;
import com.kylin.fast.openai.constant.MessageRole;
import com.kylin.fast.openai.utils.Base64Tools;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;

/**
 * Created by ZengShilin on 2023/3/2 10:52 AM
 *
 * @author ZengShilin
 */
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImgMessage implements BaseMessage {

    /**
     * MessageRole
     */
    String role;

    List<ImgMessageContent> content;


    @Override
    public String roleType() {
        return role;
    }

    @Override
    public MessageType messageType() {
        return MessageType.IMAGE;
    }


    public static ImgMessage of(MessageRole role, File image) {
        return of(role, image, "", true);
    }

    public static ImgMessage of(MessageRole role, String content, File image) {
        return of(role, image, content, true);
    }


    public static ImgMessage of(MessageRole role, String imageUrl) {
        return of(role, imageUrl, "", true);
    }

    public static ImgMessage of(MessageRole role, String content, String imageUrl) {
        return of(role, imageUrl, content, true);
    }

    public static ImgMessage of(MessageRole role, String imageUrl, String content, boolean isDetail) {
        List<ImgMessageContent> contents = Lists.newArrayList(
                ImgMessageContent.builder()
                        .type("image_url")
                        .image_url(ImageUrl.builder()
                                .url(imageUrl)
                                .detail(isDetail ? "high" : "low")
                                .build())
                        .build());
        if (StringUtils.isNotBlank(content)) {
            contents.add(ImgMessageContent.builder()
                    .type("text")
                    .text(content)
                    .build());
        }
        return ImgMessage.builder()
                .role(role.role)
                .content(contents)
                .build();
    }

    public static ImgMessage of(MessageRole role, File image, String content, boolean isDetail) {
        List<ImgMessageContent> contents = Lists.newArrayList(
                ImgMessageContent.builder()
                        .type("image_url")
                        .image_url(ImageUrl.builder()
                                .url("data:image/jpeg;base64," + Base64Tools.encodeBase64(image))
                                .detail(isDetail ? "high" : "low")
                                .build())
                        .build());
        if (StringUtils.isNotBlank(content)) {
            contents.add(ImgMessageContent.builder()
                    .type("text")
                    .text(content)
                    .build());
        }

        return ImgMessage.builder()
                .role(role.role)
                .content(contents)
                .build();
    }

}
