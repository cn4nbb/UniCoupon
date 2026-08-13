package edu.cnan.unicoupon.engine.common.context;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.Optional;


public final class UserContext {

    // 用户上下文
    private static final TransmittableThreadLocal<UserInfoDTO> USER_THREAD_LOCAL = new TransmittableThreadLocal();

    /**
     * 设置用户至上下文
     */
    public static void setUser(UserInfoDTO userInfoDTO) {
        USER_THREAD_LOCAL.set(userInfoDTO);
    }

    /**
     * 获取用户上下文中用户ID
     */
    public static String getUserId() {
        UserInfoDTO userInfoDTO = USER_THREAD_LOCAL.get();
        return Optional.ofNullable(userInfoDTO).map(UserInfoDTO::getUserId).orElse(null);
    }

    /**
     * 获取用户上下文中用户名称
     */
    public static String getUserName() {
        UserInfoDTO userInfoDTO = USER_THREAD_LOCAL.get();
        return Optional.ofNullable(userInfoDTO).map(UserInfoDTO::getUsername).orElse(null);
    }

    /**
     * 获取用户上下文店铺编号
     */
    public static Long getShopNumber() {
        UserInfoDTO userInfoDTO = USER_THREAD_LOCAL.get();
        return Optional.ofNullable(userInfoDTO).map(UserInfoDTO::getShopNumber).orElse(null);
    }

    /**
     * 清理用户上下文
     */
    public static void removerUser() {
        USER_THREAD_LOCAL.remove();
    }
}
