package edu.cnan.onecoupon.framework.config;

import edu.cnan.onecoupon.framework.web.GlobalExceptionHandler;
import org.springframework.context.annotation.Bean;


/**
 * Web 组件自动装配
 */
public class WebAutoConfiguration {

    /**
     * 构建全局异常拦截器 Bean
     */
    @Bean
    public GlobalExceptionHandler globalExceptionHandler(){return new GlobalExceptionHandler();}
}
