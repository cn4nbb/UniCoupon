package edu.cnan.onecoupon.merchant.admin.service.basics.chain;

import cn.hutool.core.collection.CollectionUtil;
import org.springframework.beans.BeansException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 商家后管责任链上下文容器
 * 实现 ApplicationContextAware 接口，允许容器拿到 ApplicationContext
 * 实现 CommandLineRunner 接口，允许容器自动执行 run 方法，填充容器
 * 自动发现责任链节点、自动分组、自动排序、自动执行的责任链框架
 */
@Component
public final class MerchantAdminChainContext<T> implements ApplicationContextAware, CommandLineRunner {

    // 通过 Spring IOC 容器获取 Bean 对象
    private ApplicationContext applicationContext;

    /**
     * 责任链容器
     * 根据 mark 对责任链节点进行分组
     * 容器内保存多条责任链
     */
    private final Map<String, List<MerchantAdminAbstractChainHandler>> abstractChainHandlerContainer = new HashMap<>();

    /**
     * 责任链执行
     *
     * @param mark         责任链组件标识
     * @param requestParam 请求参数
     */
    public void handle(String mark, T requestParam) {
        // 根据 mark 获取责任链
        List<MerchantAdminAbstractChainHandler> abstractChainHandlers = abstractChainHandlerContainer.get(mark);
        // 判断责任链是否成功拿到
        if (CollectionUtil.isEmpty(abstractChainHandlers)) {
            // 没有获取责任链则抛出异常
            throw new RuntimeException(String.format("[%s] Chain of Responsibility ID is undefined.", mark));
        }
        // 责任链开始执行
        abstractChainHandlers.forEach(each -> each.handle(requestParam));
    }

    /**
     * 填充责任链容器
     *
     * @param args
     * @throws Exception
     */
    @Override
    public void run(String... args) throws Exception {
        // 从 Spring IOC 容器中获取指定接口 Spring Bean 集合
        Map<String, MerchantAdminAbstractChainHandler> chainFilterMap = applicationContext.getBeansOfType(MerchantAdminAbstractChainHandler.class);
        // 判断 Mark 是否已经存在抽象责任链容器中，如果已经存在直接向集合新增；如果不存在，创建 Mark 和对应的集合
        chainFilterMap.forEach((beanName, bean) -> {
            List<MerchantAdminAbstractChainHandler> abstractChainHandlers = abstractChainHandlerContainer.getOrDefault(bean.mark(), new ArrayList<>());
            abstractChainHandlers.add(bean);
            abstractChainHandlerContainer.put(bean.mark(), abstractChainHandlers);
        });
        abstractChainHandlerContainer.forEach((mark, unsortedChainHandlers) -> {
            // 对每个 Mark 对应的责任链实现类集合进行排序，优先级小的在前
            unsortedChainHandlers.sort(Comparator.comparing(Ordered::getOrder));
        });
    }

    /**
     * 通过实现ApplicationContextAware接口，拿到ApplicationContext
     *
     * @param applicationContext Spring上下文
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
