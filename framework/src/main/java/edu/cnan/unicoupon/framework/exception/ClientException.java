package edu.cnan.unicoupon.framework.exception;

import edu.cnan.unicoupon.framework.errorcode.BaseErrorCode;
import edu.cnan.unicoupon.framework.errorcode.IErrorCode;

/**
 * 客户端异常｜用户发起调用请求后因客户端提交参数或其他客户端问题导致的异常
 */
public class ClientException extends AbstractException {

    public ClientException(IErrorCode iErrorCode) {
        this(null, null, iErrorCode);
    }

    public ClientException(String message) {
        this(message,null, BaseErrorCode.CLIENT_ERROR);
    }

    public ClientException(String message,IErrorCode iErrorCode) {
        this(message,null,iErrorCode);
    }

    public ClientException(String message, Throwable throwable, IErrorCode iErrorCode) {
        super(message, throwable, iErrorCode);
    }

    @Override
    public String toString() {
        return "ClientException{" +
                "code='" + errorCode + "'," +
                "message='" + errorMessage + "'" +
                '}';
    }
}
