/* ==================================================================   
 * Created [2009-7-7] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.exception.convert;

import org.apache.log4j.Logger;

import com.boubei.tss.EX;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.exception.ExceptionEncoder;
import com.boubei.tss.framework.exception.IBusinessException;
import com.boubei.tss.framework.exception.UserIdentificationException;
import com.boubei.tss.util.EasyUtils;

/** 
 * <p>
 * 默认异常转换器：加一些常见的异常转换
 * </p>
 */
public class DefaultExceptionConvertor implements IExceptionConvertor {
	
	private Logger log = Logger.getLogger(this.getClass());

    public Exception convert(Exception e) {
    	if( e != null && e.getMessage() != null) {
    		Throwable firstCause = ExceptionEncoder.getFirstCause(e);
			String firstCauseMessage = firstCause.getMessage();
			String msg = e.getMessage() + firstCause.getClass() + firstCauseMessage;
			
    		boolean needPrint = false, needRelogin = false;
    		if( e instanceof IBusinessException ) {
    			needPrint = ((IBusinessException) e).needPrint();
    			needRelogin = ((IBusinessException) e).needRelogin();
    		}
    			
			if( !(firstCause instanceof IBusinessException) && !(firstCause instanceof UserIdentificationException) ) {
				log.error(msg, firstCause);
			}
			
    		if(msg.indexOf("ConstraintViolationException") >= 0) {
    			if(msg.indexOf("cannot be null") >= 0) {
    				return new BusinessException( EX.ERR_NOT_NULL + firstCauseMessage, false );
    			}
    			if(msg.indexOf("add or update a child row: a foreign key constraint fails") >= 0) {
    				return new BusinessException( EX.ERR_FK_NOT_EXIST + firstCauseMessage, false );
    			}
    			if(msg.indexOf("insert") >= 0) {
    				return new BusinessException( EX.ERR_UNIQUE + firstCauseMessage, false);
    			}
    			else if(msg.indexOf("delete") >= 0) {
    				return new BusinessException( EX.ERR_HAS_FKEY + firstCauseMessage, false );
    			}
    			else {
    				return new BusinessException( firstCauseMessage, false );
    			}
    		}
    		
    		/* Hibernate执行出错 */
    		if(msg.indexOf("Row was updated or deleted by another transaction") >= 0) {
				return new BusinessException( EX.ERR_LOCK_VERSION + firstCauseMessage, false );
			}
    		
    		/* 原生SQL执行出错，SQLExcutor等 */
    		if(msg.indexOf("Cannot delete or update a parent row: a foreign key constraint") >= 0) {
				int index1 = firstCauseMessage.indexOf("fails") + 7;
				int index2 = firstCauseMessage.indexOf("CONSTRAINT") - 2;
				String detail = firstCauseMessage.substring( index1, Math.max(index1, index2) );
    			return new BusinessException( EX.ERR_HAS_FKEY + detail, false );
    		}
    		
    		/* 原生SQL执行出错，SQLExcutor等 */
    		if(msg.indexOf("Incorrect string value:") >= 0 && msg.indexOf("for column") >= 0 ) {
    			String errMesg = EX.ERR_EMOJI_INPUT + firstCauseMessage.substring( firstCauseMessage.indexOf("for column") + 10 );
    			return new BusinessException( errMesg, false );
    		}

    		msg = firstCauseMessage;
    		if( !needRelogin && !EasyUtils.isNullOrEmpty(msg) ) {
    			
    			// MySQL/Oracle字段不能为空
    			if(msg.indexOf("cannot be null") >= 0) {
    				msg = msg.replaceAll("Column", EX.COLUMN).replaceAll("cannot be null", EX.ERR_NOT_NULL);
    			}
        		msg = msg.replaceAll("ORA-01407:", EX.ERR_NOT_NULL);
        		
        		// 违反唯一性约束
        		msg = msg.replaceAll("Duplicate entry", EX.ERR_UNIQUE);
        		msg = msg.replaceAll("ORA-00001:", EX.ERR_UNIQUE);
        		
        		if( !msg.equals(firstCauseMessage) ) { // 如果异常Msg内容发生了改变，重新抛出异常
        			msg = msg.replaceAll("com.boubei.tss.framework.exception.BusinessException: " + EX.EXCEPTION, "");
            		msg = msg.replaceAll("com.boubei.tss.framework.exception.BusinessException:", "");
        			return new BusinessException( msg, needPrint );
        		}
    		}
    	}
        return e;
    }
}
