package edu.cnan.onecoupon.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import edu.cnan.onecoupon.framework.exception.ClientException;
import edu.cnan.onecoupon.framework.exception.ServiceException;
import edu.cnan.onecoupon.merchant.admin.common.constant.MerchantAdminRedisConstant;
import edu.cnan.onecoupon.merchant.admin.common.context.UserContext;
import edu.cnan.onecoupon.merchant.admin.dao.entity.CouponTemplateDO;
import edu.cnan.onecoupon.merchant.admin.dao.mapper.CouponTemplateMapper;
import edu.cnan.onecoupon.merchant.admin.dto.req.CouponTemplateNumberReqDTO;
import edu.cnan.onecoupon.merchant.admin.dto.req.CouponTemplatePageQueryReqDTO;
import edu.cnan.onecoupon.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import edu.cnan.onecoupon.merchant.admin.dto.resp.CouponTemplatePageQueryRespDTO;
import edu.cnan.onecoupon.merchant.admin.dto.resp.CouponTemplateQueryRespDTO;
import edu.cnan.onecoupon.merchant.admin.common.enums.ChainBizMarkEnum;
import edu.cnan.onecoupon.merchant.admin.common.enums.CouponTemplateStatusEnum;
import edu.cnan.onecoupon.merchant.admin.mq.event.CouponTemplateDelayEvent;
import edu.cnan.onecoupon.merchant.admin.mq.producer.CouponTemplateDelayExecuteStatusProducer;
import edu.cnan.onecoupon.merchant.admin.service.CouponTemplateService;
import edu.cnan.onecoupon.merchant.admin.service.basics.chain.MerchantAdminChainContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RBloomFilter;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 优惠券模板业务逻辑实现层
 * 继承 ServiceImpl 接口，拥有 MyBatis-Plus 提供的通用 CRUD 方法
 * 绑定 Mapper 和 实体类类型
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class CouponTemplateServiceImpl extends ServiceImpl<CouponTemplateMapper, CouponTemplateDO> implements CouponTemplateService {

    private final CouponTemplateMapper couponTemplateMapper;
    private final MerchantAdminChainContext merchantAdminChainContext;
    private final StringRedisTemplate stringRedisTemplate;
    private final CouponTemplateDelayExecuteStatusProducer couponTemplateDelayExecuteStatusProducer;
    private final RBloomFilter<String> couponTemplateQueryBloomFilter;

    @LogRecord(success = """
                    创建优惠券：{{#requestParam.name}}， \
                    优惠对象：{COMMON_ENUM_PARSE{'DiscountTargetEnum' + '_' + #requestParam.target}}， \
                    优惠类型：{COMMON_ENUM_PARSE{'DiscountTypeEnum' + '_' + #requestParam.type}}， \
                    库存数量：{{#requestParam.stock}}， \
                    优惠商品编码：{{#requestParam.goods}}， \
                    有效期开始时间：{{#requestParam.validStartTime}}， \
                    有效期结束时间：{{#requestParam.validEndTime}}， \
                    领取规则：{{#requestParam.receiveRule}}， \
                    消耗规则：{{#requestParam.consumeRule}};
                    """,
            type = "CouponTemplate",
            bizNo = "{{#bizNo}}",
            extra = "{{#requestParam.toString()}}"
    )
    @Override
    public void createCouponTemplate(CouponTemplateSaveReqDTO requestParam) {
        // 责任链验证请求参数
        merchantAdminChainContext.handle(ChainBizMarkEnum.MERCHANT_ADMIN_CREATE_COUPON_TEMPLATE_KEY.name(), requestParam);

        // 新增优惠券模板信息到数据库
        CouponTemplateDO couponTemplateDO = BeanUtil.toBean(requestParam, CouponTemplateDO.class);
        couponTemplateDO.setStatus(CouponTemplateStatusEnum.ACTIVE.getStatus()); // 设置优惠券为启用状态
        couponTemplateDO.setShopNumber(UserContext.getShopNumber()); // 设置优惠券所属商户编号
        couponTemplateMapper.insert(couponTemplateDO);

        LogRecordContext.putVariable("bizNo",couponTemplateDO.getId());

        // 缓存预热：通过将数据库的记录序列化成 JSON 字符串放入 Redis 缓存
        CouponTemplateQueryRespDTO actualRespDTO = BeanUtil.toBean(couponTemplateDO, CouponTemplateQueryRespDTO.class);
        Map<String, Object> cacheTargetMap = BeanUtil.beanToMap(actualRespDTO); // 将响应实体对象转为Map类型
        Map<String, String> actualCacheTargetMap = cacheTargetMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() != null ? entry.getValue().toString() : ""
                ));// Redis Hash 需要 Map<String,String>
        String couponTemplateCacheKey = String.format(MerchantAdminRedisConstant.COUPON_TEMPLATE_KEY, couponTemplateDO.getId());

        // 通过 LUA 脚本执行设置 Hash 数据以及设置过期时间
        String luaScript = "redis.call('HMSET',KEYS[1],unpack(ARGV,1,#ARGV-1))" +
                "redis.call('EXPIREAT',KEYS[1],#ARGV)";
        List<String> keys = Collections.singletonList(couponTemplateCacheKey); // 构造KEYS单元素数组
        List<String> args = new ArrayList<>(actualCacheTargetMap.size() * 2 + 1); // 构造ARGV数组(k1,v1,k2,v2,...,expireTime)
        actualCacheTargetMap.forEach((key, value) -> {
            args.add(key);
            args.add(value);
        });
        // 优惠券活动过期时间转换为秒级别的 Unix 时间戳
        args.add(String.valueOf(couponTemplateDO.getValidEndTime().getTime()/1000));

        // 执行LUA脚本
        stringRedisTemplate.execute(
                new DefaultRedisScript<>(luaScript,Long.class), // 设置脚本最后一条语句执行完毕后返回值为Long类型
                keys,
                args.toArray() // 必须是数组对象
        );

        // 发送延时消息事件，优惠券活动到期修改优惠券模板状态
        CouponTemplateDelayEvent couponTemplateDelayEvent = CouponTemplateDelayEvent.builder()
                .couponTemplateId(couponTemplateDO.getId())
                .shopNumber(UserContext.getShopNumber())
                .delayTime(couponTemplateDO.getValidEndTime().getTime())
                .build();
        couponTemplateDelayExecuteStatusProducer.sendMessage(couponTemplateDelayEvent);

        // 优惠券模板添加到布隆过滤器
        couponTemplateQueryBloomFilter.add(String.valueOf(couponTemplateDO.getId()));
    }

    @Override
    public IPage<CouponTemplatePageQueryRespDTO> pageQueryCouponTemplate(CouponTemplatePageQueryReqDTO requestParam) {

        // 条件构造器
        LambdaQueryWrapper<CouponTemplateDO> wrapper = Wrappers.lambdaQuery(CouponTemplateDO.class)
                .eq(CouponTemplateDO::getShopNumber, UserContext.getShopNumber())
                .like(StrUtil.isNotBlank(requestParam.getName()), CouponTemplateDO::getName, requestParam.getName())
                .like(StrUtil.isNotBlank(requestParam.getGoods()), CouponTemplateDO::getGoods, requestParam.getGoods())
                .eq(ObjectUtil.isNotEmpty(requestParam.getType()), CouponTemplateDO::getType, requestParam.getType())
                .eq(ObjectUtil.isNotEmpty(requestParam.getTarget()), CouponTemplateDO::getTarget, requestParam.getTarget());

        // mybatis-plus 分页查询
        IPage<CouponTemplateDO> selectPage = couponTemplateMapper.selectPage(requestParam, wrapper);

        // 将数据库持久层对象转换为分页查询响应实体
        return selectPage.convert(each -> BeanUtil.toBean(each, CouponTemplatePageQueryRespDTO.class));
    }

    @Override
    public CouponTemplateQueryRespDTO findCouponTemplateById(String couponTemplateId) {

        // 条件构造器
        LambdaQueryWrapper<CouponTemplateDO> wrapper = Wrappers.lambdaQuery(CouponTemplateDO.class)
                .eq(CouponTemplateDO::getShopNumber, UserContext.getShopNumber())
                .eq(CouponTemplateDO::getId, couponTemplateId);

        CouponTemplateDO couponTemplateDO = couponTemplateMapper.selectOne(wrapper);
        return BeanUtil.toBean(couponTemplateDO,CouponTemplateQueryRespDTO.class);
    }

    @LogRecord(success = "增加发行量：{{#requestParam.getNumber}}",
            type = "CouponTemplate",
            bizNo = "{{#requestParam.couponTemplateId}}"
    )
    @Override
    public void increaseNumberCouponTemplate(CouponTemplateNumberReqDTO requestParam) {

        // 横向越权判断 增加非自己商铺的优惠券模板发行量
        LambdaQueryWrapper<CouponTemplateDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateDO.class)
                .eq(CouponTemplateDO::getShopNumber, UserContext.getShopNumber())
                .eq(StrUtil.isNotBlank(requestParam.getCouponTemplateId()), CouponTemplateDO::getId, requestParam.getCouponTemplateId());
        CouponTemplateDO couponTemplateDO = couponTemplateMapper.selectOne(queryWrapper);
        if (couponTemplateDO == null) {
            throw new ClientException("优惠券模板异常，请检查操作是否正确...");
        }

        // 验证优惠券模板状态
        if (ObjectUtil.equal(couponTemplateDO.getStatus(),CouponTemplateStatusEnum.ENDED)) {
            throw new ClientException("优惠券已结束");
        }

        // 保存修改前数据
        LogRecordContext.putVariable("originalData", JSON.toJSONString(couponTemplateDO));

        // 增加优惠券模板库存
        int result = couponTemplateMapper.increaseNumberCouponTemplate(couponTemplateDO.getShopNumber(),requestParam.getCouponTemplateId(),requestParam.getNumber());
        if (!SqlHelper.retBool(result)) {
            throw new ServiceException("增加优惠券模板库存失败");
        }

        // 更新缓存
        String couponTemplateKey = String.format(MerchantAdminRedisConstant.COUPON_TEMPLATE_KEY,requestParam.getCouponTemplateId());
        stringRedisTemplate.opsForHash().increment(couponTemplateKey,"stock",requestParam.getNumber());
    }

    @LogRecord(success = "结束优惠券",
            type = "CouponTemplateId",
            bizNo = "{{#couponTemplateId}}"
    )
    @Override
    public void terminateCouponTemplate(String couponTemplateId) {

        // 横向越权校验
        LambdaQueryWrapper<CouponTemplateDO> wrapper = Wrappers.lambdaQuery(CouponTemplateDO.class)
                .eq(CouponTemplateDO::getShopNumber, UserContext.getShopNumber())
                .eq(CouponTemplateDO::getId, couponTemplateId);
        CouponTemplateDO couponTemplateDO = couponTemplateMapper.selectOne(wrapper);
        if (couponTemplateDO == null) {
            // 一旦 couponTemplateDO 为空，则证明存在越权操作
            throw new ClientException("优惠券模板异常");
        }

        // 检测优惠券模板状态是否已结束
        if (ObjectUtil.equal(couponTemplateDO.getStatus(),CouponTemplateStatusEnum.ENDED.getStatus())) {
            throw new ClientException("优惠券模板已结束");
        }

        // 修改优惠券模板为结束状态
        CouponTemplateDO updateCouponTemplateDO = CouponTemplateDO.builder()
                .status(CouponTemplateStatusEnum.ENDED.getStatus())
                .build();
        LambdaUpdateWrapper<CouponTemplateDO> updateWrapper = Wrappers.lambdaUpdate(CouponTemplateDO.class)
                .eq(CouponTemplateDO::getId, couponTemplateId)
                .eq(CouponTemplateDO::getShopNumber, UserContext.getShopNumber());
        couponTemplateMapper.update(updateCouponTemplateDO,updateWrapper);

        // 更新缓存
        String couponTemplateCache = String.format(MerchantAdminRedisConstant.COUPON_TEMPLATE_KEY,couponTemplateId);
        stringRedisTemplate.opsForHash().put(couponTemplateCache,"status",String.valueOf(CouponTemplateStatusEnum.ENDED.getStatus()));
    }

}
