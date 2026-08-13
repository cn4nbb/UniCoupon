package edu.cnan.unicoupon.merchant.admin.mq.producer;

import cn.hutool.core.util.StrUtil;
import edu.cnan.unicoupon.merchant.admin.mq.base.BaseSendExtendDTO;
import edu.cnan.unicoupon.merchant.admin.mq.base.MessageWrapper;
import edu.cnan.unicoupon.merchant.admin.mq.event.CouponTaskExecuteEvent;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 优惠券推送任务执行生产者
 */
@Component
public class CouponTaskActualExecuteProducer extends AbstractCommonSendProducerTemplate<CouponTaskExecuteEvent>{

    private final ConfigurableEnvironment configurableEnvironment;

    public CouponTaskActualExecuteProducer(@Autowired RocketMQTemplate rocketMQTemplate,@Autowired ConfigurableEnvironment configurableEnvironment) {
        super(rocketMQTemplate);
        this.configurableEnvironment = configurableEnvironment;
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendDTO(CouponTaskExecuteEvent messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("优惠券推送执行")
                .topic(configurableEnvironment.resolvePlaceholders("uni-coupon_distribution-service_coupon-task-execute_topic${unique-name:}"))
                .keys(String.valueOf(messageSendEvent.getCouponTaskId()))
                .sendTimeout(2000L)
                .build();
    }

    @Override
    protected Message<?> buildMessage(CouponTaskExecuteEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        String keys = StrUtil.isEmpty(requestParam.getKeys())?UUID.randomUUID().toString():requestParam.getKeys();

        return MessageBuilder
                .withPayload(new MessageWrapper(keys,messageSendEvent))
                .setHeader(MessageConst.PROPERTY_KEYS,keys)
                .setHeader(MessageConst.PROPERTY_TAGS,requestParam.getTag())
                .build();
    }
}
