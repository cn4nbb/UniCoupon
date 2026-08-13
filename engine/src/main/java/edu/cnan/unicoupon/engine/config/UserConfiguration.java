package edu.cnan.unicoupon.engine.config;

import edu.cnan.unicoupon.engine.common.context.UserContext;
import edu.cnan.unicoupon.engine.common.context.UserInfoDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UserConfiguration implements WebMvcConfigurer {

    /**
     * 用户信息传输拦截器
     */
    @Bean
    public UserHandlerInterceptor userHandlerInterceptor() {
        return new UserHandlerInterceptor();
    }

    /**
     * 添加用户信息传输拦截器至相关拦截路径
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userHandlerInterceptor())
                .addPathPatterns("/**");
    }

    /**
     * 用户信息传输拦截器
     */
    static class UserHandlerInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            // 模拟用户模块
            UserInfoDTO userInfoDTO = new UserInfoDTO("1810518709471555585", "pdd45305558318", 1810714735922956666L);
            UserContext.setUser(userInfoDTO);
            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
            UserContext.removerUser();
        }
    }
}
