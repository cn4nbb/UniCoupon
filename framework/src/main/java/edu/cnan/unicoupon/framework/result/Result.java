package edu.cnan.unicoupon.framework.result;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 定义全局返回对象｜方便接口参数返回约束，避免不同的参会定义混淆前端接收
 */
@Data
@Accessors(chain = true) // getter和setter返回对象本身，支持链式编程
public class Result<T> implements Serializable {

    // 序列化版本号
    @Serial
    private static final long serialVersionUID = 1l;

    // 正确返回码
    public static final String SUCCESS_CODE = "0";

    // 返回码
    private String code;

    // 返回消息
    private String message;

    // 响应数据
    private T data;

    // 请求ID
    private String requestId;

    /**
     * 判断响应是否成功
     */
    public boolean isSuccess() {
        return SUCCESS_CODE.equals(code);
    }
}
