package edu.cnan.unicoupon.merchant.admin.mq.producer;

import cn.hutool.core.util.StrUtil;
import edu.cnan.unicoupon.merchant.admin.mq.base.BaseSendExtendDTO;
import edu.cnan.unicoupon.merchant.admin.mq.base.MessageWrapper;
import edu.cnan.unicoupon.merchant.admin.mq.event.CouponTemplateDelayEvent;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CouponTemplateDelayExecuteStatusProducer extends AbstractCommonSendProducerTemplate<CouponTemplateDelayEvent>{

    private final ConfigurableEnvironment environment;

    public CouponTemplateDelayExecuteStatusProducer(@Autowired RocketMQTemplate rocketMQTemplate,@Autowired ConfigurableEnvironment environment) {
        super(rocketMQTemplate);
        this.environment = environment;
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendDTO(CouponTemplateDelayEvent messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("优惠券模板关闭定时执行")
                .topic(environment.resolvePlaceholders("uni-coupon_merchant-admin-service_coupon-template-delay_topic${unique-name:}"))
                .keys(String.valueOf(messageSendEvent.getCouponTemplateId()))
                .delayTime(messageSendEvent.getDelayTime())
                .build();
    }

    @Override
    protected Message<?> buildMessage(CouponTemplateDelayEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        String keys = StrUtil.isEmpty(requestParam.getKeys())? UUID.randomUUID().toString():requestParam.getKeys();
        return MessageBuilder
                .withPayload(new MessageWrapper(keys,messageSendEvent))
                .setHeader(MessageConst.PROPERTY_KEYS,keys)
                .setHeader(MessageConst.PROPERTY_TAGS,requestParam.getTag())
                .build();
    }
}
