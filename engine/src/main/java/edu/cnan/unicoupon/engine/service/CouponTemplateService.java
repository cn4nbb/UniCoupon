package edu.cnan.unicoupon.engine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.cnan.unicoupon.engine.dao.entity.CouponTemplateDO;
import edu.cnan.unicoupon.engine.dto.req.CouponTemplateQueryReqDTO;
import edu.cnan.unicoupon.engine.dto.resp.CouponTemplateQueryRespDTO;

/**
 * 优惠券模板业务逻辑层
 */
public interface CouponTemplateService extends IService<CouponTemplateDO> {

    /**
     * 查询优惠券模板详情
     */
    CouponTemplateQueryRespDTO findCouponTemplateById(CouponTemplateQueryReqDTO requestParam);
}
