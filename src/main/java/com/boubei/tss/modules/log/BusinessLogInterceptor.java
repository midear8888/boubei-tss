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

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Table;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boubei.tss.framework.persistence.IEntity;
import com.boubei.tss.util.EasyUtils;

import freemarker.template.TemplateException;

/**
 * 记录业务日志的拦截器
 * 
 */
@Component("businessLogInterceptor")
public class BusinessLogInterceptor implements MethodInterceptor {

    protected Logger log = Logger.getLogger(this.getClass());

    /** 业务日志处理对象 */
    @Autowired private IBusinessLogger businessLogger;
 
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method targetMethod = invocation.getMethod(); /* 获取目标方法 */
        Object[] args = invocation.getArguments(); /* 获取目标方法的参数 */
        
        Long preTime = System.currentTimeMillis();
        Object returnVal = invocation.proceed(); /* 调用目标方法的返回值 */
        
        int methodExcuteTime = (int) (System.currentTimeMillis() - preTime);

        Logable annotation = targetMethod.getAnnotation(Logable.class); // 取得注释对象
        if (annotation != null) {

            String operateTable = annotation.operateObject();
            String operateInfo = annotation.operateInfo();
            
            String operateMethod = targetMethod.getName();
            
            returnVal = EasyUtils.checkNull(returnVal, "null");
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("returnVal", returnVal);
            
            Table table = returnVal.getClass().getAnnotation(Table.class);
            if( table != null ) {
            	if( "${table}".equals(operateTable) ) {
            		operateTable = table.name();
            	}
            	data.put("tableName", operateTable);
            }
            if( returnVal instanceof IEntity ) {
            	operateMethod += ", " + ((IEntity)returnVal).getPK();
            }

            Log log = new Log(operateMethod, parseMacro(operateInfo, args, data));
            log.setOperateTable(operateTable);
            log.setMethodExcuteTime(methodExcuteTime);

            businessLogger.output(log);
        }

        return returnVal;
    }

    /**
     * 解析日志中的宏代码
     * 
     * @param logInfo
     * @param args
     * @return
     * @throws IOException
     * @throws TemplateException
     */
    public String parseMacro(String logInfo, Object[] args, Map<String, Object> data) {
        if(args != null && args.length > 0) {
    		Object[] tempArgs = new Object[args.length];
    		for( int i = 0; i < args.length; i++ ) {
    			tempArgs[i] = args[i] == null ? "" : args[i];
    		}
    		 data.put("args", tempArgs);
    	}

        logInfo = EasyUtils.fmParse(logInfo, data);
        return logInfo;
    }
}