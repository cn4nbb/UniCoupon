package edu.cnan.unicoupon.merchant.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import edu.cnan.unicoupon.framework.idempotent.NoDuplicateSubmit;
import edu.cnan.unicoupon.framework.result.Result;
import edu.cnan.unicoupon.framework.web.Results;
import edu.cnan.unicoupon.merchant.admin.dto.req.CouponTemplateNumberReqDTO;
import edu.cnan.unicoupon.merchant.admin.dto.req.CouponTemplatePageQueryReqDTO;
import edu.cnan.unicoupon.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import edu.cnan.unicoupon.merchant.admin.dto.resp.CouponTemplatePageQueryRespDTO;
import edu.cnan.unicoupon.merchant.admin.dto.resp.CouponTemplateQueryRespDTO;
import edu.cnan.unicoupon.merchant.admin.service.CouponTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 优惠券模板控制层
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "优惠券模板管理")
public class CouponTemplateController {

    private final CouponTemplateService couponTemplateService;

    @NoDuplicateSubmit
    @Operation(summary = "商家创建优惠券模板")
    @PostMapping("/api/merchant-admin/coupon-template/create")
    public Result<Void> createCouponTemplate(@RequestBody CouponTemplateSaveReqDTO requestParam) {
        couponTemplateService.createCouponTemplate(requestParam);
        return Results.success();
    }

    @Operation(summary = "分页查询优惠券模板")
    @GetMapping("/api/merchant-admin/coupon-template/page")
    public Result<IPage<CouponTemplatePageQueryRespDTO>> pageQueryCouponTemplate(CouponTemplatePageQueryReqDTO requestParam) {
        return Results.success(couponTemplateService.pageQueryCouponTemplate(requestParam));
    }

    @Operation(summary = "查询优惠券模板详情")
    @GetMapping("/api/merchant-admin/coupon-template/find")
    public Result<CouponTemplateQueryRespDTO> findCouponTemplate(String couponTemplateId){
        return Results.success(couponTemplateService.findCouponTemplateById(couponTemplateId));
    }

    @Operation(summary = "增加优惠券模板发行量")
    @NoDuplicateSubmit(message = "请勿短时间内重复增加优惠券发行量")
    @PostMapping("/api/merchant-admin/coupon-template/increase-number")
    public Result<Void> increaseNumberCouponTemplate(@RequestBody CouponTemplateNumberReqDTO requestParam) {
        couponTemplateService.increaseNumberCouponTemplate(requestParam);
        return Results.success();
    }

    @Operation(summary = "结束优惠券模板")
    @PostMapping("/api/merchant-admin/coupon-template/terminate")
    public Result<Void> terminateCouponTemplate(String couponTemplateId) {
        couponTemplateService.terminateCouponTemplate(couponTemplateId);
        return Results.success();
    }
}
