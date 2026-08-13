package edu.cnan.unicoupon.distribution.common.constant;

/**
 * 优惠券 Redis 常量类
 */
public final class EngineRedisConstant {

    /**
     * 优惠券模板缓存 Key
     */
    public static final String COUPON_TEMPLATE_KEY = "uni-coupon_engine:template:%s";

    /**
     * 用户已领取优惠券列表模板 Key
     */
    public static final String USER_COUPON_TEMPLATE_LIST_KEY = "uni-coupon_engine:user-template-list:%s";

    /**
     * 限制用户领取优惠券模板次数缓存 Key
     */
    public static final String USER_COUPON_TEMPLATE_LIMIT_KEY = "uni-coupon_engine:user-template-limit:";
}
