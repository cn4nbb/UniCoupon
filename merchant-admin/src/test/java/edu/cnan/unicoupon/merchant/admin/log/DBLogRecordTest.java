package edu.cnan.unicoupon.merchant.admin.log;

import com.alibaba.fastjson2.JSONObject;
import edu.cnan.unicoupon.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import edu.cnan.unicoupon.merchant.admin.service.CouponTemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 操作日志测试
 */
@SpringBootTest
public class DBLogRecordTest {

    @Autowired
    private CouponTemplateService couponTemplateService;

    @Test
    public void dbLogRecordTest() {
        JSONObject receiveRule = new JSONObject();
        receiveRule.put("limitPerPerson", 1); // 每人限领
        receiveRule.put("usageInstructions", "使用说明"); // 使用说明
        JSONObject consumeRule = new JSONObject();
        consumeRule.put("termsOfUse", new BigDecimal("10")); // 使用条件 满 x 元可用
        consumeRule.put("maximumDiscountAmount", new BigDecimal("3")); // 最大优惠金额
        consumeRule.put("explanationOfUnmetC 3onditions", "不满足使用条件说明"); // 不满足使用条件说明
        consumeRule.put("validityPeriod", 48); // 自领取优惠券后有效时间，单位小时

        CouponTemplateSaveReqDTO couponTemplateSaveReqDTO = new CouponTemplateSaveReqDTO();
        couponTemplateSaveReqDTO.setName("商品立减券");
        couponTemplateSaveReqDTO.setSource(0);
        couponTemplateSaveReqDTO.setTarget(1);
        couponTemplateSaveReqDTO.setType(0);
        couponTemplateSaveReqDTO.setValidStartTime(new Date());
        couponTemplateSaveReqDTO.setValidEndTime(new Date());
        couponTemplateSaveReqDTO.setStock(20000);
        couponTemplateSaveReqDTO.setReceiveRule(receiveRule.toString());
        couponTemplateSaveReqDTO.setConsumeRule(consumeRule.toString());

        couponTemplateService.createCouponTemplate(couponTemplateSaveReqDTO);
    }
}