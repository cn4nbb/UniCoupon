package edu.cnan.onecoupon.engine.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 布隆过滤器配置类
 */
@Configuration
public class RBloomFilterConfiguration {

    /**
     * 优惠券查询缓存穿透布隆过滤器
     */
    @Bean
    public RBloomFilter<String> rBloomFilter(RedissonClient redissonClient){
        RBloomFilter<String> couponTemplateBloomFilter = redissonClient.getBloomFilter("couponTemplateQueryBloomFilter");
        // 初始化过滤器的容量和错误率
        couponTemplateBloomFilter.tryInit(640L,0.001);
        return couponTemplateBloomFilter;
    }
}
