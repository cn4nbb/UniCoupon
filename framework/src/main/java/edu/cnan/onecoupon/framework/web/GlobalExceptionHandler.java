package edu.cnan.onecoupon.framework.web;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import edu.cnan.onecoupon.framework.errorcode.BaseErrorCode;
import edu.cnan.onecoupon.framework.exception.AbstractException;
import edu.cnan.onecoupon.framework.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Optional;

/**
 * 全局异常拦截器 | 拦截指定异常并通过优雅构建方式返回前端信息
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 拦截参数验证异常
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public Result validExceptionHandler(HttpServletRequest request, MethodArgumentNotValidException ex) {

        // 获取异常的绑定结果
        BindingResult bindingResult = ex.getBindingResult();
        // 获取异常信息的首条
        FieldError firstFieldError = CollectionUtil.getFirst(bindingResult.getFieldErrors());
        // 构造日志输出的异常信息
        String exceptionStr = Optional.ofNullable(firstFieldError)
                .map(FieldError::getDefaultMessage)
                .orElse(StrUtil.EMPTY);
        // 日志输出异常信息
        log.error("[{}] {} [ex] {}", request.getMethod(), getUrl(request), exceptionStr);

        return Results.failure(BaseErrorCode.CLIENT_ERROR.code(), exceptionStr);
    }

    /**
     * 拦截应用内抛出的异常
     */
    @ExceptionHandler(value = AbstractException.class)
    public Result abstractExceptionHandler(HttpServletRequest request, AbstractException ex) {

        if (ex.getCause() != null) {
            log.error("[{}] {} [ex] {}",request.getMethod(),request.getRequestURL().toString(),ex,ex.getCause());
            return Results.failure(ex);
        }
        StringBuilder stackTraceBuilder = new StringBuilder();
        stackTraceBuilder.append(ex.getClass().getName()).append(": ").append(ex.getErrorMessage()).append("\n");
        StackTraceElement[] stackTrace = ex.getStackTrace();
        for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
            stackTraceBuilder.append("\tat ").append(stackTrace[i]).append("\n");
        }
        log.error("[{}] {} [ex] {} \n\n{}", request.getMethod(), request.getRequestURL().toString(), ex, stackTraceBuilder);
        return Results.failure(ex);
    }

    /**
     * 拦截未知异常
     */
    @ExceptionHandler(value = Throwable.class)
    public Result defaultExceptionHandler(HttpServletRequest request,Throwable throwable) {
        log.error("[{}] {} ", request.getMethod(), getUrl(request), throwable);
        return Results.failure();
    }


    /**
     * 获取请求URL
     */
    private String getUrl(HttpServletRequest request) {
        if (request.getQueryString().isEmpty()) {
            return request.getRequestURL().toString();
        }
        return request.getRequestURL().toString() + "?" + request.getQueryString();
    }
}
