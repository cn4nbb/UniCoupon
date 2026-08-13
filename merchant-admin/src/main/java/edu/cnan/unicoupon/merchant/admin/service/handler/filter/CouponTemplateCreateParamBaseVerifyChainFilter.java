package edu.cnan.unicoupon.merchant.admin.service.handler.filter;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import edu.cnan.unicoupon.framework.exception.ClientException;
import edu.cnan.unicoupon.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import edu.cnan.unicoupon.merchant.admin.common.enums.ChainBizMarkEnum;
import edu.cnan.unicoupon.merchant.admin.common.enums.DiscountTargetEnum;
import edu.cnan.unicoupon.merchant.admin.common.enums.DiscountTypeEnum;
import edu.cnan.unicoupon.merchant.admin.service.basics.chain.MerchantAdminAbstractChainHandler;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;

/**
 * 验证优惠券创建接口参数是否正确责任链｜验证参数基本数据关系是否正确
 */
@Component
public class CouponTemplateCreateParamBaseVerifyChainFilter implements MerchantAdminAbstractChainHandler<CouponTemplateSaveReqDTO> {

    // 最大库存数量
    private final int maxStock = 20000000;

    /**
     * 责任链执行逻辑 | 验证参数基本数据关系是否正确
     *
     * @param requestParam
     */
    @Override
    public void handle(CouponTemplateSaveReqDTO requestParam) {

        // 判断优惠对象是否正确
        boolean targetMatch = Arrays.stream(DiscountTargetEnum.values())
                .anyMatch(discountTargetEnum -> discountTargetEnum.getType() == requestParam.getTarget());
        if (!targetMatch) {
            throw new ClientException("优惠对象不存在");
        }

        // 判断优惠对象与优惠商品的关系是否正确
        if (ObjectUtil.equal(requestParam.getTarget(), DiscountTargetEnum.ALL_STORE_GENERAL.getType())
                && StrUtil.isNotEmpty(requestParam.getGoods())) {
            throw new ClientException("优惠券全店通用不可设置指定商品");
        }
        if (ObjectUtil.equal(requestParam.getTarget(), DiscountTargetEnum.PRODUCT_SPECIFIC.getType())
                && StrUtil.isEmpty(requestParam.getGoods())) {
            throw new ClientException("优惠券未设置指定商品");
        }

        // 判断优惠类型是否正确
        boolean typeMatch = Arrays.stream(DiscountTypeEnum.values())
                .anyMatch(discountTypeEnum -> discountTypeEnum.getType() == requestParam.getType());
        if (!typeMatch) {
            throw new ClientException("优惠类型不存在");
        }

        // 判断有效期开始时间是否正确
        Date now = new Date();
        if (requestParam.getValidStartTime().before(now)) {
            // throw new ClientException("有效期开始时间不可以早于当前时间");
        }

        // 判断库存数量是否正确
        if (requestParam.getStock() <=0 || requestParam.getStock() > maxStock) {
            throw new ClientException("库存数量设置异常");
        }

        // 判断JSON参数格式是否正确
        if (!JSON.isValid(requestParam.getConsumeRule())) {
            throw new ClientException("消耗规则格式错误");
        }
        if (!JSON.isValid(requestParam.getReceiveRule())) {
            throw new ClientException("领取规则格式错误");
        }
    }

    /**
     * @return 责任链组件标识
     */
    @Override
    public String mark() {
        return ChainBizMarkEnum.MERCHANT_ADMIN_CREATE_COUPON_TEMPLATE_KEY.name();
    }

    /**
     * @return 组件在责任链中的次序
     */
    @Override
    public int getOrder() {
        return 10;
    }
}
