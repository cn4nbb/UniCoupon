package edu.cnan.unicoupon.distribution.mq.producer;

import cn.hutool.core.util.StrUtil;
import edu.cnan.unicoupon.distribution.mq.base.BaseSendExtendDTO;
import edu.cnan.unicoupon.distribution.mq.base.MessageWrapper;
import edu.cnan.unicoupon.distribution.mq.event.CouponTemplateDistributionEvent;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Component
public class CouponExecuteDistributionProducer extends AbstractCommonSendProducerTemplate<CouponTemplateDistributionEvent> {

    private final ConfigurableEnvironment environment;

    public CouponExecuteDistributionProducer(@Autowired RocketMQTemplate rocketMQTemplate, @Autowired ConfigurableEnvironment environment) {
        super(rocketMQTemplate);
        this.environment = environment;
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendDTO(CouponTemplateDistributionEvent messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("优惠券发放执行")
                .keys(String.valueOf(messageSendEvent.getCouponTaskId()))
                .topic(environment.resolvePlaceholders("uni-coupon_distribution-service_coupon-execute-distribution_topic${unique-name:}"))
                .sendTimeout(2000L)
                .build();
    }

    @Override
    protected Message<?> buildMessage(CouponTemplateDistributionEvent event, BaseSendExtendDTO requestParam) {
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        return MessageBuilder
                .withPayload(new MessageWrapper(keys, event))
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                .build();
    }
}
