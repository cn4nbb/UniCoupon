package edu.cnan.unicoupon.engine.common.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录信息实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoDTO {

    // 用户ID
    private String userId;

    // 用户名
    private String username;

    // 店铺编号
    private Long shopNumber;
}
