package edu.cnan.unicoupon.engine.common.constant;

/**
 * 优惠券 Redis 常量类
 */
public final class EngineRedisConstant {

    /**
     * 优惠券模板缓存 Key
     */
    public static final String COUPON_TEMPLATE_KEY = "uni-coupon_engine:template:%s";

    /**
     * 优惠券模板缓存空值 Key
     */
    public static final String COUPON_TEMPLATE_IS_NULL_KEY = "uni-coupon_engine:template_is_null:%s";

    /**
     * 优惠券模板缓存分布式锁 Key
     */
    public static final String LOCK_COUPON_TEMPLATE_KEY = "uni-coupon_engine:lock:template:%s";
}
