package edu.cnan.onecoupon.merchant.admin.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 优惠券优惠对象枚举
 */
@RequiredArgsConstructor
public enum DiscountTargetEnum {

    /**
     * 商品专属优惠
     */
    PRODUCT_SPECIFIC(0, "商品专属优惠"),
    /**
     * 全店通用优惠
     */
    ALL_STORE_GENERAL(1, "全店通用优惠");

    @Getter
    private final int type;

    @Getter
    private final String value;


    /**
     * 根据 type 找到对应的 value
     */
    public static String getValueByType(int type) {
        for (DiscountTargetEnum target : DiscountTargetEnum.values()) {
            if (target.getType() == type ){
                return target.getValue();
            }
        }
        throw new IllegalArgumentException();
    }
}
