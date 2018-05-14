/* ==================================================================   
 * Created [2016-06-22] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 忽略日志
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface LogDisable {
    
}
