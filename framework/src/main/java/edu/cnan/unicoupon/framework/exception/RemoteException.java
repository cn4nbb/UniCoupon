package edu.cnan.unicoupon.framework.exception;

import edu.cnan.unicoupon.framework.errorcode.BaseErrorCode;
import edu.cnan.unicoupon.framework.errorcode.IErrorCode;

/**
 * 远程服务调用异常｜比如订单调用支付失败，向上抛出的异常应该是远程服务调用异常
 */
public class RemoteException extends AbstractException {

    public RemoteException(IErrorCode iErrorCode) {
        this(null, null, iErrorCode);
    }

    public RemoteException(String message) {
        this(message, null, BaseErrorCode.REMOTE_ERROR);
    }

    public RemoteException(String message, IErrorCode iErrorCode) {
        this(message, null, iErrorCode);
    }

    public RemoteException(String message, Throwable throwable, IErrorCode iErrorCode) {
        super(message, throwable, iErrorCode);
    }

    @Override
    public String toString() {
        return "RemoteException{" +
                "code='" + errorCode + "'," +
                "message='" + errorMessage + "'" +
                '}';
    }
}
