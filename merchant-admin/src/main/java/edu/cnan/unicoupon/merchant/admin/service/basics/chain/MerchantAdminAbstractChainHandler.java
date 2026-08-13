package edu.cnan.unicoupon.merchant.admin.service.basics.chain;

import org.springframework.core.Ordered;

/**
 * 抽象商家后管业务责任链组件
 */
public interface MerchantAdminAbstractChainHandler<T> extends Ordered {

    /**
     * 执行责任链逻辑
     * @param requestParam
     */
    void handle(T requestParam);

    /**
     * @return 责任链组件标识
     */
    String mark();
}
