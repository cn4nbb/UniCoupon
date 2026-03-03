package edu.cnan.onecoupon.framework.exception;

import edu.cnan.onecoupon.framework.errorcode.BaseErrorCode;
import edu.cnan.onecoupon.framework.errorcode.IErrorCode;

/**
 * 服务端运行异常｜请求运行过程中出现的不符合业务预期的异常
 */
public class ServiceException extends AbstractException {

    public ServiceException(IErrorCode iErrorCode) {
        this(null, null, iErrorCode);
    }

    public ServiceException(String message) {
        this(message, null, BaseErrorCode.SERVICE_ERROR);
    }

    public ServiceException(String message, IErrorCode iErrorCode) {
        this(message, null, iErrorCode);
    }

    public ServiceException(String message, Throwable throwable, IErrorCode iErrorCode) {
        super(message, throwable, iErrorCode);
    }

    @Override
    public String toString() {
        return "ServiceException{" +
                "code='" + errorCode + "'," +
                "message='" + errorMessage + "'" +
                '}';
    }
}
