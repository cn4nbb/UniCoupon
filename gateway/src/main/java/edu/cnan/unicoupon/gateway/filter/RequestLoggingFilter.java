package edu.cnan.unicoupon.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 请求日志过滤器
 */
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    // 创建日志记录器对象
    private static final Logger LOG = LoggerFactory.getLogger(RequestLoggingFilter.class);

    /**
     * 记录请求日志和追踪请求链路
     *
     * @param exchange
     * @param chain
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取请求对象
        ServerHttpRequest request = exchange.getRequest();
        // 获取请求类型
        HttpMethod method = request.getMethod();
        // 生成该请求的唯一UUID，用于后续追踪请求
        String traceId = UUID.randomUUID().toString();
        // 记录请求时间
        long startTime = System.currentTimeMillis();


        // MDC为日志记录提供线程本地的上下文，基于ThreadLocal实现
        // 存储追踪ID
        MDC.put("traceId", traceId);

        // 日志输出请求信息
        LOG.info("请求URL：{}", request.getURI());
        LOG.info("请求类型：{}", method);
        LOG.info("请求头：{}", request.getHeaders());

        // 如果是GET类型，则输出请求参数
        if (method == HttpMethod.GET) {
            LOG.info("请求参数：{}", request.getQueryParams());
        }

        // 将该请求放行到下游服务，请求响应结束后执行then()
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            // 计算响应时间
            long duration = System.currentTimeMillis() - startTime;
            LOG.info("响应时间：{}", duration);
        }));
    }

    /**
     * 设定在过滤器链中的执行顺序
     */
    @Override
    public int getOrder() {
        // 返回值越小，优先级越高
        return -1;
    }
}
