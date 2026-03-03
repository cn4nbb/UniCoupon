package edu.cnan.onecoupon.framework.web;

import edu.cnan.onecoupon.framework.errorcode.BaseErrorCode;
import edu.cnan.onecoupon.framework.exception.AbstractException;
import edu.cnan.onecoupon.framework.result.Result;

import java.util.Optional;

/**
 * 构建全局返回对象构造器｜Result工具类
 */
public final class Results {

    /**
     * 构造成功响应
     */
    public static Result<Void> success() {
        return new Result<Void>()
                .setCode(Result.SUCCESS_CODE);
    }

    /**
     * 构造带返回数据的成功响应
     */
    public static <T> Result<T> success(T data) {
        return new Result<T>()
                .setCode(Result.SUCCESS_CODE)
                .setData(data);
    }

    /**
     * 构造服务端失败响应
     */
    protected static Result<Void> failure() {
        return new Result<Void>()
                .setCode(BaseErrorCode.CLIENT_ERROR.code())
                .setMessage(BaseErrorCode.CLIENT_ERROR.message());
    }

    /**
     * 通过 AbstractException 构造失败响应
     */
    protected static Result<Void> failure(AbstractException abstractException) {
        return new Result<Void>()
                .setCode(Optional.ofNullable(abstractException.getErrorCode()).orElse(BaseErrorCode.SERVICE_ERROR.code()))
                .setMessage(Optional.ofNullable(abstractException.getErrorMessage()).orElse(BaseErrorCode.SERVICE_ERROR.message()));
    }

    /**
     * 通过 errorCode errorMessage 构建失败响应
     */
    protected static Result<Void> failure(String errorCode,String errorMessage) {
        return new Result<Void>()
                .setCode(errorCode)
                .setMessage(errorMessage);
    }
}
