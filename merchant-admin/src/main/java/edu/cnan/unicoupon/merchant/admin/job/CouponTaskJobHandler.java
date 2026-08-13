package edu.cnan.unicoupon.merchant.admin.job;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.XxlJob;
import edu.cnan.unicoupon.merchant.admin.common.enums.CouponTaskSendTypeEnum;
import edu.cnan.unicoupon.merchant.admin.common.enums.CouponTaskStatusEnum;
import edu.cnan.unicoupon.merchant.admin.dao.entity.CouponTaskDO;
import edu.cnan.unicoupon.merchant.admin.dao.mapper.CouponTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 优惠券推送任务扫描定时发送记录 XXL-Job 处理器
 */
@Component
@RequiredArgsConstructor
public class CouponTaskJobHandler extends IJobHandler {

    private final CouponTaskMapper couponTaskMapper;
    private static final int MAX_LIMIT = 100;

    /**
     * 任务处理逻辑
     */
    @XxlJob("couponTemplateTask")
    @Override
    public void execute() throws Exception {
        long initId = 0;
        Date now = new Date();

        while (true) {
            // 获取到达发送时间的定时发送任务
            List<CouponTaskDO> couponTaskDOList = fetchPendingTasks(initId, now);

            if (CollUtil.isEmpty(couponTaskDOList)) break;

            // 分发定时任务，修改任务状态
            for (CouponTaskDO each : couponTaskDOList) {
                distributeCoupon(each);
            }

            // 如果小于限制 则说明后续已无更多任务 直接退出循环
            if (couponTaskDOList.size() < MAX_LIMIT) break;

            // 更新 initId
            initId = couponTaskDOList.stream()
                    .mapToLong(CouponTaskDO::getId)
                    .max()
                    .orElse(initId);
        }
    }

    /**
     * 分发定时任务，修改任务状态
     */
    public void distributeCoupon(CouponTaskDO couponTask) {
        CouponTaskDO couponTaskDO = CouponTaskDO.builder()
                .id(couponTask.getId())
                .status(CouponTaskStatusEnum.IN_PROGRESS.getStatus())
                .build();

        couponTaskMapper.updateById(couponTaskDO);
    }

    /**
     * 查询数据库获取到达发送时间的定时推送任务
     */
    public List<CouponTaskDO> fetchPendingTasks(long initId, Date now) {
        LambdaQueryWrapper<CouponTaskDO> queryWrapper = Wrappers.lambdaQuery(CouponTaskDO.class)
                .eq(CouponTaskDO::getSendType, CouponTaskSendTypeEnum.SCHEDULED.getType())
                .le(CouponTaskDO::getSendTime, now)
                .gt(CouponTaskDO::getId, initId)
                .last("LIMIT "+ MAX_LIMIT);

        return couponTaskMapper.selectList(queryWrapper);
    }
}
