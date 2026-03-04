package edu.cnan.onecoupon.distribution.mq.base;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

/**
 * 消息体包装器
 */
@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public final class MessageWrapper<T> implements Serializable {

    private static final Long serialVersionUID = 1L;

    /**
     * 消息标识
     */
    @NonNull
    private String keys;

    /**
     * 消息体
     */
    @NonNull
    private T message;

    /**
     * 唯一标识，用于客户端幂等验证
     */
    private String uuid = UUID.randomUUID().toString();

    /**
     * 消息发送时间
     */
    private Long timeStamp = System.currentTimeMillis();
}
