package edu.cnan.unicoupon.merchant.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.cnan.unicoupon.merchant.admin.dao.entity.CouponTaskDO;
import edu.cnan.unicoupon.merchant.admin.dto.req.CouponTaskCreateReqDTO;

/**
 * 优惠券推送业务逻辑层
 */
public interface CouponTaskService extends IService<CouponTaskDO> {

    /**
     * 商家创建优惠券推送任务
     * @param requestParam 请求参数
     */
    void createCouponTask(CouponTaskCreateReqDTO requestParam);
}
