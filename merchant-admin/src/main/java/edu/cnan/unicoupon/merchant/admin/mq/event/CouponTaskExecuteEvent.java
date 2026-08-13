package edu.cnan.unicoupon.merchant.admin.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 优惠券推送任务执行事件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponTaskExecuteEvent{

    /**
     * 推送任务Id
     */
    private Long couponTaskId;
}
