package edu.cnan.onecoupon.merchant.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import edu.cnan.onecoupon.merchant.admin.dao.entity.CouponTemplateDO;
import edu.cnan.onecoupon.merchant.admin.dto.req.CouponTemplateNumberReqDTO;
import edu.cnan.onecoupon.merchant.admin.dto.req.CouponTemplatePageQueryReqDTO;
import edu.cnan.onecoupon.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import edu.cnan.onecoupon.merchant.admin.dto.resp.CouponTemplatePageQueryRespDTO;
import edu.cnan.onecoupon.merchant.admin.dto.resp.CouponTemplateQueryRespDTO;

/**
 * 优惠券模板业务逻辑层
 */
public interface CouponTemplateService extends IService<CouponTemplateDO> {

    /**
     * 创建商家优惠券模板
     * @param requestParam 新增参数
     */
    void createCouponTemplate(CouponTemplateSaveReqDTO requestParam);

    /**
     * 分页查询优惠券模板
     * @param requestParam 查询参数
     */
    IPage<CouponTemplatePageQueryRespDTO> pageQueryCouponTemplate(CouponTemplatePageQueryReqDTO requestParam);

    /**
     * 查询优惠券模板详情
     * @param couponTemplateId 优惠券模板Id
     */
    CouponTemplateQueryRespDTO findCouponTemplateById(String couponTemplateId);

    /**
     * 增加优惠券模板发行量
     * @param requestParam
     */
    void increaseNumberCouponTemplate(CouponTemplateNumberReqDTO requestParam);

    /**
     * 结束优惠券模板
     * @param couponTemplateId 优惠券模板Id
     */
    void terminateCouponTemplate(String couponTemplateId);
}
