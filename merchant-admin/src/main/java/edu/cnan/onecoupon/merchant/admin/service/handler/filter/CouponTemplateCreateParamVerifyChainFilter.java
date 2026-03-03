package edu.cnan.onecoupon.merchant.admin.service.handler.filter;

import cn.hutool.core.util.ObjectUtil;
import edu.cnan.onecoupon.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import edu.cnan.onecoupon.merchant.admin.common.enums.ChainBizMarkEnum;
import edu.cnan.onecoupon.merchant.admin.common.enums.DiscountTargetEnum;
import edu.cnan.onecoupon.merchant.admin.service.basics.chain.MerchantAdminAbstractChainHandler;
import org.springframework.stereotype.Component;

/**
 * 验证优惠券创建接口参数是否正确责任链｜验证参数数据是否正确
 */
@Component
public class CouponTemplateCreateParamVerifyChainFilter implements MerchantAdminAbstractChainHandler<CouponTemplateSaveReqDTO> {

    /**
     * 责任链执行逻辑 | 验证参数数据是否正确
     * @param requestParam
     */
    @Override
    public void handle(CouponTemplateSaveReqDTO requestParam) {
        if (ObjectUtil.equal(requestParam.getTarget(), DiscountTargetEnum.PRODUCT_SPECIFIC.getType())) {
            // 调用商品中台判断商品是否存在，不存在则抛出异常
        }
    }

    @Override
    public String mark() {
        return ChainBizMarkEnum.MERCHANT_ADMIN_CREATE_COUPON_TEMPLATE_KEY.name();
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
