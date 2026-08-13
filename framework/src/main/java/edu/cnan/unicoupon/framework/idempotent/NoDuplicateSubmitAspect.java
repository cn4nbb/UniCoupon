package edu.cnan.unicoupon.framework.idempotent;

import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import edu.cnan.unicoupon.framework.exception.ClientException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 防止用户重复提交表单信息切面控制器
 */
@Aspect
@RequiredArgsConstructor
public final class NoDuplicateSubmitAspect {

    private final RedissonClient redissonClient;

    /**
     * 增强方法标记 {@link NoDuplicateSubmit} 注解逻辑
     */
    @Around("@annotation(edu.cnan.unicoupon.framework.idempotent.NoDuplicateSubmit)")
    public Object noDuplicateSubmit(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取分布式锁标识
        String lockKey = String.format("no-duplicate-submit:path:%s:currentUserId:%s:md5:%s",getServletPath(),getUserId(),calcArgsMD5(joinPoint));

        RLock lock = redissonClient.getLock(lockKey);
        // 尝试获取锁 获取不到就代表重复提交 直接抛出异常
        if (!lock.tryLock()) {
            throw new ClientException(getNoDuplicateSubmit(joinPoint).message());
        }

        Object result;
        // 执行原方法逻辑
        try {
            result = joinPoint.proceed();
        } finally {
            lock.unlock();
        }
        return result;
    }

    /**
     * @return 获取 NoDuplicateSubmit 注解实例
     */
    private NoDuplicateSubmit getNoDuplicateSubmit(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
        Method declaredMethod = joinPoint.getTarget().getClass().getDeclaredMethod(methodSignature.getName(), methodSignature.getParameterTypes());

        return declaredMethod.getAnnotation(NoDuplicateSubmit.class);
    }

    /**
     * @return 获取当地线程的 ServletPath
     */
    private String getServletPath() {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return sra.getRequest().getServletPath();
    }

    /**
     * @return 获取当地线程的 用户id
     */
    private String getUserId() {
        // 用户属于非核心功能，这里先通过模拟的形式代替。后续如果需要后管展示，会重构该代码
        return "1810518709471555585";
    }

    /**
     * @return 请求参数的md5
     */
    private String calcArgsMD5(ProceedingJoinPoint joinPoint){
        return DigestUtil.md5Hex(JSON.toJSONBytes(joinPoint.getArgs()));
    }
}
