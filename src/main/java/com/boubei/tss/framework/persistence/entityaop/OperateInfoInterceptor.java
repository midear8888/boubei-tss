/* ==================================================================   
 * Created [2009-7-7] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.persistence.entityaop;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.stereotype.Component;

import com.boubei.tss.framework.persistence.IDao;
import com.boubei.tss.framework.persistence.IEntity;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.util.EasyUtils;

/**
 * <p>
 *  对象操作者信息记录拦截器
 * </p>
 */
@Component("operateInfoInterceptor")
public class OperateInfoInterceptor extends MatchByDaoMethodNameInterceptor {
	
    protected int judgeManipulateKind(String methodName){
        if(match(methodName, Arrays.asList( (updateKind + ",refresh").split(","))))
            return UPDATE;
        
        return super.judgeManipulateKind(methodName);
    }

	public Object invoke(MethodInvocation invocation) throws Throwable {
		Object target = invocation.getThis();
		Object[] args = invocation.getArguments();
		args = (Object[]) EasyUtils.checkNull(args, new Object[]{});
		
        for (int i = 0; i < args.length; i++) {
            int manipulateKind = judgeManipulateKind(invocation.getMethod().getName());
            if (args[i] instanceof IOperatable 
            		&& (manipulateKind == SAVE || manipulateKind == UPDATE)) {
               
                IOperatable opObj = (IOperatable) args[i];
                Serializable pk = ((IEntity)opObj).getPK();
                
				if( pk == null ) { // ID为null，说明是新建
                    opObj.setCreateTime(new Date());
                    opObj.setCreatorId(Environment.getUserId());
                    opObj.setCreatorName(Environment.getUserName());  
                    
                    // 定时器写数据时，域信息已经指定
                    String domain = (String) EasyUtils.checkNull( opObj.getDomain(), Environment.getDomain() );
                    opObj.setDomain(domain);
                } 
                else {
                    opObj.setUpdateTime(new Date());
                    opObj.setUpdatorId(Environment.getUserId());
                    opObj.setUpdatorName(Environment.getUserName());  
                    
                    /* 修改后，createTime的时分秒没了（日期传递到前台时截去了时分秒，保存后就没有了），
                     * update时不要前台传入的createTime，而是从DB查出来复制回去
                     */
                    @SuppressWarnings("unchecked")
                    IDao<IEntity> dao = (IDao<IEntity>) target;
                    IOperatable old = (IOperatable) dao.getEntity( opObj.getClass(), pk);
                    old = (IOperatable) EasyUtils.checkNull(old, opObj); // 可能修改时记录已被其它人[删除]
                    opObj.setCreateTime(old.getCreateTime());
                }
            }
        }
			
        return invocation.proceed();
	}
}

	