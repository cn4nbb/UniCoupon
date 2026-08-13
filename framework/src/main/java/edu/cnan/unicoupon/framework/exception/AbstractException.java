package edu.cnan.unicoupon.framework.exception;

import edu.cnan.unicoupon.framework.errorcode.IErrorCode;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 抽象异常类 | 三类异常体系，客户端异常、服务端异常、远程服务调用异常
 */
@Getter
public abstract class AbstractException extends RuntimeException {

    // 异常码
    public final String errorCode;

    // 异常信息
    public final String errorMessage;

    public AbstractException(String message, Throwable throwable, IErrorCode iErrorCode) {

        // 调用父类构造器
        super(message,throwable);

        this.errorCode = iErrorCode.code();

        // 如果未传入异常信息，那么就默认设置异常码对应的异常信息
        this.errorMessage = Optional.ofNullable(StringUtils.hasLength(message) ? message : null).orElse(iErrorCode.message());
    }
}
