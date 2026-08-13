package edu.cnan.unicoupon.merchant.admin.mq.base;

import lombok.*;
import java.io.Serializable;

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
     * 消息发送时间
     */
    private Long timeStamp = System.currentTimeMillis();
}
