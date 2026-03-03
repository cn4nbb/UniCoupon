package edu.cnan.onecoupon.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.thread.RejectPolicy;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.cnan.onecoupon.framework.exception.ClientException;
import edu.cnan.onecoupon.merchant.admin.common.context.UserContext;
import edu.cnan.onecoupon.merchant.admin.common.enums.CouponTaskSendTypeEnum;
import edu.cnan.onecoupon.merchant.admin.common.enums.CouponTaskStatusEnum;
import edu.cnan.onecoupon.merchant.admin.dao.entity.CouponTaskDO;
import edu.cnan.onecoupon.merchant.admin.dao.mapper.CouponTaskMapper;
import edu.cnan.onecoupon.merchant.admin.dto.req.CouponTaskCreateReqDTO;
import edu.cnan.onecoupon.merchant.admin.mq.event.CouponTaskExecuteEvent;
import edu.cnan.onecoupon.merchant.admin.mq.producer.CouponTaskActualExecuteProducer;
import edu.cnan.onecoupon.merchant.admin.service.CouponTaskService;
import edu.cnan.onecoupon.merchant.admin.service.CouponTemplateService;
import edu.cnan.onecoupon.merchant.admin.service.handler.excel.RowCountListener;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.*;

/**
 * 优惠券推送业务逻辑实现层
 */
@Service
@RequiredArgsConstructor
public class CouponTaskServiceImpl extends ServiceImpl<CouponTaskMapper, CouponTaskDO> implements CouponTaskService {

    private final RedissonClient redissonClient;
    private final CouponTemplateService couponTemplateService;
    private final CouponTaskMapper couponTaskMapper;
    private final CouponTaskActualExecuteProducer couponTaskActualExecuteProducer;

    /**
     * 异步执行 Excel 解析线程池
     * 核心数：CPU核心数
     * 最大线程数：CPU核心数*2
     * 工作队列：SynchronousQueue 不存任务，有任务立即交给线程执行
     * 拒绝策略：丢弃，有延迟队列兜底
     */
    private final ExecutorService executorService = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors() << 1,
            60,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new ThreadPoolExecutor.DiscardPolicy()
    );

    /**
     * 解析 Excel 获取行数，并刷新数据库
     * @param jsonObject Excel文件地址，CouponTaskId
     */
    private void refreshCouponTaskNum(JSONObject jsonObject){
        RowCountListener rowCountListener = new RowCountListener();
        EasyExcel.read(jsonObject.getString("fileAddress"),rowCountListener).sheet().doRead();
        int rowNum = rowCountListener.getRowCount();

        CouponTaskDO couponTaskDO = CouponTaskDO.builder()
                .id(jsonObject.getLong("couponTaskId"))
                .sendNum(rowNum)
                .build();
        couponTaskMapper.updateById(couponTaskDO);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createCouponTask(CouponTaskCreateReqDTO requestParam) {
        // 判断请求参数是否有效
        if (couponTemplateService.findCouponTemplateById(requestParam.getCouponTemplateId()) == null) {
            throw new ClientException("优惠券模板不存在，请检查提交信息是否正确");
        }
        // 判断请求参数约束关系是否正确
        if (ObjectUtil.equal(requestParam.getSendType(), CouponTaskSendTypeEnum.IMMEDIATE.getType())
                && ObjectUtil.isNotEmpty(requestParam.getSendTime())) {
            throw new ClientException("优惠券批次任务为立即发送，发送时间不可设置。");
        }
        if (ObjectUtil.equal(requestParam.getSendType(), CouponTaskSendTypeEnum.SCHEDULED.getType())
                && ObjectUtil.isEmpty(requestParam.getSendTime())) {
            throw new ClientException("优惠券批次任务缺少定时时间。");
        }

        // 转换为数据库持久层对象
        CouponTaskDO couponTaskDO = BeanUtil.copyProperties(requestParam, CouponTaskDO.class);
        couponTaskDO.setBatchId(IdUtil.getSnowflakeNextId());
        couponTaskDO.setShopNumber(UserContext.getShopNumber());
        couponTaskDO.setOperatorId(Long.parseLong(UserContext.getUserId()));
        couponTaskDO.setStatus(ObjectUtil.equal(couponTaskDO.getSendType(), CouponTaskSendTypeEnum.IMMEDIATE.getType())
                ? CouponTaskStatusEnum.IN_PROGRESS.getStatus() : CouponTaskStatusEnum.PENDING.getStatus());

        // 保存优惠券推送任务记录到数据库
        couponTaskMapper.insert(couponTaskDO);

        // 构建 JSONObject
        JSONObject jsonObject = JSONObject.of("fileAddress",requestParam.getFileAddress(),"couponTaskId",couponTaskDO.getId());
        // 线程池异步解析 Excel 数据
        // 100 万数据大概需要 4 秒才能返回前端，如果加上验证将会时间更长，所以这里将最耗时的统计操作异步化
        executorService.execute(() -> refreshCouponTaskNum(jsonObject));

        // 创建阻塞队列
        RBlockingDeque<Object> blockingDeque = redissonClient.getBlockingDeque("COUPON_TASK_SEND_NUM_DELAY_QUEUE");
        // 创建延迟队列，到时间后将任务提交给阻塞队列
        RDelayedQueue<Object> delayedQueue = redissonClient.getDelayedQueue(blockingDeque);
        // 这里延迟时间设置 20 秒，原因是我们笃定上面线程池 20 秒之内就能结束任务
        delayedQueue.offer(jsonObject,20,TimeUnit.SECONDS);

        // 如果是立即发送任务，直接调用消息队列进行发送流程
        if (ObjectUtil.equal(requestParam.getSendType(),CouponTaskSendTypeEnum.IMMEDIATE.getType())) {
            // 执行优惠券推送业务，正式向用户发放优惠券
            CouponTaskExecuteEvent couponTaskExecuteEvent = CouponTaskExecuteEvent.builder()
                    .couponTaskId(couponTaskDO.getId())
                    .build();

            couponTaskActualExecuteProducer.sendMessage(couponTaskExecuteEvent);
        }
    }

    /**
     * 优惠券延迟刷新发送条数兜底消费者｜这是兜底策略，一般来说不会执行这段逻辑
     * 如果延迟消息没有持久化成功，或者 Redis 挂了怎么办？后续可以人工处理
     */
    @Service
    @RequiredArgsConstructor
    class CouponTaskDelay implements CommandLineRunner{

        private final RedissonClient redissonClient;
        private final CouponTaskMapper couponTaskMapper;

        /**
         * 一直阻塞等待 Redis 延迟队列里的任务，
         * 一旦任务到期就检查数据库，
         * 如果发现 sendNum 还没更新，就补做一次统计，
         * 无论发生什么异常，这个线程都不会停
         */
        @Override
        public void run(String... args) throws Exception {
            Executors.newSingleThreadExecutor(
                    runnable -> {
                        Thread thread = new Thread(runnable);
                        thread.setName("delay_coupon-task_send-num_consumer");
                        thread.setDaemon(true);
                        return thread;
                    }
            ).execute(() -> {
                RBlockingDeque<JSONObject> blockingDeque = redissonClient.getBlockingDeque("COUPON_TASK_SEND_NUM_DELAY_QUEUE");

                try {
                    for (;;) {
                        // 获取延迟队列已经到时间的值
                        JSONObject jsonObject = blockingDeque.take();
                        if (jsonObject != null) {
                            Long couponTaskId = jsonObject.getLong("couponTaskId");
                            CouponTaskDO couponTaskDO = couponTaskMapper.selectById(couponTaskId);
                            // 获取优惠券推送记录，查看发送条数是否已经有值，有的话代表上面线程池已经处理完成，无需再处理
                            if (couponTaskDO.getSendNum() == null) {
                                refreshCouponTaskNum(jsonObject);
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }

            });
        }
    }
}
