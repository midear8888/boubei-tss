package com.boubei.tss.cache.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)

/**
 * SHORT|functionName(param1,
 * 支持以本条数据的值解析
 * eg {domain:D006, order_id:9},
 * 则配置SHORT|getOrderCode(${order_id},  会先解析成getOrderCode(9,  再去清除缓存
 * 支持系统参数配置清除缓存，如实体为DomainConfig，则配置DomainConfigCacheClear的内容（多个用#分隔）也会被读取
 */
public @interface CacheClear {
    String[] values() default {};
}
