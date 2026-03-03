package edu.cnan.onecoupon.engine.controller;

import edu.cnan.onecoupon.engine.dto.req.CouponTemplateQueryReqDTO;
import edu.cnan.onecoupon.framework.result.Result;
import edu.cnan.onecoupon.framework.web.Results;
import edu.cnan.onecoupon.engine.dto.resp.CouponTemplateQueryRespDTO;
import edu.cnan.onecoupon.engine.service.CouponTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 优惠券模板控制层
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "优惠券模板管理")
public class CouponTemplateController {

    private final CouponTemplateService couponTemplateService;

    @Operation(summary = "查询优惠券模板详情")
    @GetMapping("/api/engine/coupon-template/query")
    public Result<CouponTemplateQueryRespDTO> findCouponTemplate(CouponTemplateQueryReqDTO requestParam){
        return Results.success(couponTemplateService.findCouponTemplateById(requestParam));
    }

}
