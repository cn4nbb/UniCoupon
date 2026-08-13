package edu.cnan.unicoupon.merchant.admin.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 布隆过滤器配置
 */
@Configuration
public class RBloomFilterConfiguration {

    /**
     * 优惠券查询缓存穿透布隆过滤器
     */
    @Bean
    public RBloomFilter<String> couponTemplateBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> couponTemplateBloomFilter = redissonClient.getBloomFilter("couponTemplateQueryBloomFilter");
        couponTemplateBloomFilter.tryInit(640L,0.001);
        return couponTemplateBloomFilter;
    }
}
