package edu.cnan.unicoupon.engine.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Date;

/**
 * 数据库持久层配置类｜配置 MyBatis-Plus 相关分页插件等
 */
@Configuration
public class DataBaseConfiguration {

    /**
     * MyBatis-Plus MySQL 分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(){
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
        mybatisPlusInterceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return mybatisPlusInterceptor;
    }

    /**
     * MyBatis-Plus 源数据自定填充类
     */
    @Bean
    public MyMetaObjectHandler myMetaObjectHandler() {
        return new MyMetaObjectHandler();
    }

    /**
     * MyBatis-Plus 源数据自定填充类
     */
    static class MyMetaObjectHandler implements MetaObjectHandler{

        @Override
        public void insertFill(MetaObject metaObject) {
            strictInsertFill(metaObject,"createTime",Date::new,Date.class);
            strictInsertFill(metaObject,"updateTime",Date::new,Date.class);
            strictInsertFill(metaObject,"delFlag",() -> 0,Integer.class);
        }

        @Override
        public void updateFill(MetaObject metaObject) {
            strictUpdateFill(metaObject,"updateTime",Date::new,Date.class);
        }
    }
}
