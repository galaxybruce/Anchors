package com.galaxybruce.android.anchors.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 初始化任务注解
 * <p>
 * modification history:
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface TaskAnchor {

    String[] depends() default ""; // 依赖task id

    String description() default ""; // 描述

}
