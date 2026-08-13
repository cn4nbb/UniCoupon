package edu.cnan.unicoupon.engine.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.map.MapUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.cnan.unicoupon.engine.common.constant.EngineRedisConstant;
import edu.cnan.unicoupon.engine.common.enums.CouponTemplateStatusEnum;
import edu.cnan.unicoupon.engine.dao.entity.CouponTemplateDO;
import edu.cnan.unicoupon.engine.dao.mapper.CouponTemplateMapper;
import edu.cnan.unicoupon.engine.dto.req.CouponTemplateQueryReqDTO;
import edu.cnan.unicoupon.engine.dto.resp.CouponTemplateQueryRespDTO;
import edu.cnan.unicoupon.engine.service.CouponTemplateService;
import edu.cnan.unicoupon.framework.exception.ClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final RBloomFilter<String> couponTemplateQueryBloomFilter;

    @Override
    public CouponTemplateQueryRespDTO findCouponTemplateById(CouponTemplateQueryReqDTO requestParam) {
        // 先查绚缓存中是否存在数据
        String couponTemplateRedisKey = String.format(EngineRedisConstant.COUPON_TEMPLATE_KEY, requestParam.getCouponTemplateId());
        Map<Object, Object> couponTemplateCacheMap = stringRedisTemplate.opsForHash().entries(couponTemplateRedisKey);

        // 如果存在直接返回，不存在需要通过布隆过滤器、缓存空值以及双重判定锁的形式读取数据库中的记录
        if (MapUtil.isEmpty(couponTemplateCacheMap)) {

            // 查询布隆过滤器中不存在 直接抛出异常
            if (!couponTemplateQueryBloomFilter.contains(requestParam.getCouponTemplateId())) {
                throw new ClientException("优惠券模板不存在");
            }

            // 查询缓存中是否存在空值缓存
            String couponTemplateIsNullKey = String.format(EngineRedisConstant.COUPON_TEMPLATE_IS_NULL_KEY, requestParam.getCouponTemplateId());
            Boolean hasKeyFlag = stringRedisTemplate.hasKey(couponTemplateIsNullKey);
            if (hasKeyFlag) {
                throw new ClientException("优惠券模板不存在");
            }

            // CouponTemplateId 作为分布式锁的唯一标识
            String lockCouponTemplateKey = String.format(EngineRedisConstant.LOCK_COUPON_TEMPLATE_KEY, requestParam.getCouponTemplateId());
            RLock lock = redissonClient.getLock(lockCouponTemplateKey);

            lock.lock();
            try {
                // 双重判定空值缓存是否存在，存在则继续抛异常
                hasKeyFlag = stringRedisTemplate.hasKey(couponTemplateIsNullKey);
                if (hasKeyFlag) {
                    throw new ClientException("优惠券模板不存在");
                }

                // 通过双重判定锁优化大量请求无意义查询数据库
                couponTemplateCacheMap = stringRedisTemplate.opsForHash().entries(couponTemplateRedisKey);
                // 查询数据库
                if (MapUtil.isEmpty(couponTemplateCacheMap)) {
                    LambdaQueryWrapper<CouponTemplateDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateDO.class)
                            .eq(CouponTemplateDO::getId, Long.parseLong(requestParam.getCouponTemplateId()))
                            .eq(CouponTemplateDO::getShopNumber, Long.parseLong(requestParam.getShopNumber()))
                            .eq(CouponTemplateDO::getStatus, CouponTemplateStatusEnum.ACTIVE.getStatus());

                    CouponTemplateDO couponTemplateDO = couponTemplateMapper.selectOne(queryWrapper);

                    // 如果数据库中查询不到 则直接报错
                    if (couponTemplateDO == null) {
                        // 缓存空值
                        stringRedisTemplate.opsForValue().set(couponTemplateIsNullKey,"");
                        throw new ClientException("查询优惠券已过期或不存在");
                    }

                    // 将数据库实体类转为响应实体类
                    CouponTemplateQueryRespDTO actualRespDTO = BeanUtil.toBean(couponTemplateDO, CouponTemplateQueryRespDTO.class);
                    Map<String, Object> cacheTargetMap = BeanUtil.beanToMap(actualRespDTO,false,true);
                    // Redis 要求写入数据必须都是 String 类型
                    Map<String, String> actualCacheTargetMap = cacheTargetMap.entrySet()
                            .stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue() != null ? entry.getValue().toString() : ""));


                    // 通过 LUA 脚本执行设置 Hash 数据以及设置过期时间
                    String luaScript = "redis.call('HMSET', KEYS[1], unpack(ARGV, 1, #ARGV - 1)) " +
                            "redis.call('EXPIREAT', KEYS[1], ARGV[#ARGV])";

                    List<String> keys = Collections.singletonList(couponTemplateRedisKey);
                    List<String> args = new ArrayList<>(actualCacheTargetMap.size()*2+1);

                    actualCacheTargetMap.forEach((key,value) -> {
                                args.add(key);
                                args.add(value);
                            });

                    // 优惠券活动过期时间转换为秒级别的 Unix 时间戳
                    args.add(String.valueOf(couponTemplateDO.getValidEndTime().getTime()/1000));

                    // 执行 LUA 脚本
                    stringRedisTemplate.execute(
                            new DefaultRedisScript<>(luaScript),
                            keys,
                            args
                    );

                    couponTemplateCacheMap = cacheTargetMap.entrySet()
                            .stream()
                            .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue));
                }
            } finally {
                lock.unlock();
            }
        }

        return BeanUtil.mapToBean(couponTemplateCacheMap,CouponTemplateQueryRespDTO.class,false, CopyOptions.create());
    }

    /**
     * 缓存击穿解决方案
     */
    public CouponTemplateQueryRespDTO findCouponTemplateV1(CouponTemplateQueryReqDTO requestParam) {
        // 查询 Redis 缓存中是否存在优惠券模板信息
        String couponTemplateCacheKey = String.format(EngineRedisConstant.COUPON_TEMPLATE_KEY, requestParam.getCouponTemplateId());
        Map<Object, Object> couponTemplateCacheMap = stringRedisTemplate.opsForHash().entries(couponTemplateCacheKey);

        // 如果存在直接返回，不存在需要通过双重判定锁的形式读取数据库中的记录
        if (MapUtil.isEmpty(couponTemplateCacheMap)) {
            // 获取优惠券模板分布式锁
            // 关于缓存击穿更多注释事项
            RLock lock = redissonClient.getLock(String.format(EngineRedisConstant.LOCK_COUPON_TEMPLATE_KEY, requestParam.getCouponTemplateId()));
            lock.lock();

            try {
                // 通过双重判定锁优化大量请求无意义查询数据库
                couponTemplateCacheMap = stringRedisTemplate.opsForHash().entries(couponTemplateCacheKey);
                if (MapUtil.isEmpty(couponTemplateCacheMap)) {
                    LambdaQueryWrapper<CouponTemplateDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateDO.class)
                            .eq(CouponTemplateDO::getShopNumber, Long.parseLong(requestParam.getShopNumber()))
                            .eq(CouponTemplateDO::getId, Long.parseLong(requestParam.getCouponTemplateId()))
                            .eq(CouponTemplateDO::getStatus, CouponTemplateStatusEnum.ACTIVE.getStatus());
                    CouponTemplateDO couponTemplateDO = couponTemplateMapper.selectOne(queryWrapper);

                    // 优惠券模板不存在或者已过期直接抛出异常
                    if (couponTemplateDO == null) {
                        throw new ClientException("优惠券模板不存在或已过期");
                    }

                    // 通过将数据库的记录序列化成 JSON 字符串放入 Redis 缓存
                    CouponTemplateQueryRespDTO actualRespDTO = BeanUtil.toBean(couponTemplateDO, CouponTemplateQueryRespDTO.class);
                    Map<String, Object> cacheTargetMap = BeanUtil.beanToMap(actualRespDTO, false, true);
                    Map<String, String> actualCacheTargetMap = cacheTargetMap.entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    entry -> entry.getValue() != null ? entry.getValue().toString() : ""
                            ));

                    // 通过 LUA 脚本执行设置 Hash 数据以及设置过期时间
                    String luaScript = "redis.call('HMSET', KEYS[1], unpack(ARGV, 1, #ARGV - 1)) " +
                            "redis.call('EXPIREAT', KEYS[1], ARGV[#ARGV])";

                    List<String> keys = Collections.singletonList(couponTemplateCacheKey);
                    List<String> args = new ArrayList<>(actualCacheTargetMap.size() * 2 + 1);
                    actualCacheTargetMap.forEach((key, value) -> {
                        args.add(key);
                        args.add(value);
                    });

                    // 优惠券活动过期时间转换为秒级别的 Unix 时间戳
                    args.add(String.valueOf(couponTemplateDO.getValidEndTime().getTime() / 1000));

                    // 执行 LUA 脚本
                    stringRedisTemplate.execute(
                            new DefaultRedisScript<>(luaScript, Long.class),
                            keys,
                            args.toArray()
                    );
                    couponTemplateCacheMap = cacheTargetMap.entrySet()
                            .stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                }
            } finally {
                lock.unlock();
            }
        }

        return BeanUtil.mapToBean(couponTemplateCacheMap, CouponTemplateQueryRespDTO.class, false, CopyOptions.create());
    }

    /**
     * 缓存穿透解决方案之布隆过滤器
     */
    public CouponTemplateQueryRespDTO findCouponTemplateV2(CouponTemplateQueryReqDTO requestParam) {
        // 判断布隆过滤器是否存在指定模板 ID，不存在直接返回错误
        if (!couponTemplateQueryBloomFilter.contains(requestParam.getCouponTemplateId())) {
            throw new ClientException("优惠券模板不存在");
        }

        LambdaQueryWrapper<CouponTemplateDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateDO.class)
                .eq(CouponTemplateDO::getShopNumber, Long.parseLong(requestParam.getShopNumber()))
                .eq(CouponTemplateDO::getId, Long.parseLong(requestParam.getCouponTemplateId()))
                .eq(CouponTemplateDO::getStatus, CouponTemplateStatusEnum.ACTIVE.getStatus());
        CouponTemplateDO couponTemplateDO = couponTemplateMapper.selectOne(queryWrapper);

        return BeanUtil.toBean(couponTemplateDO, CouponTemplateQueryRespDTO.class);
    }

}
