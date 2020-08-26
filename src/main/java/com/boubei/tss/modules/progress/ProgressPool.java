/* ==================================================================   
 * Created [2016-06-22] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.progress;

import java.util.HashMap;
import java.util.Map;

import com.boubei.tss.EX;
import com.boubei.tss.cache.Cacheable;
import com.boubei.tss.cache.Pool;
import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.Environment;

/**
 * <p> 进度条池 </p>
 * <p>
 * 根据进度对象的isConceal属性来判断执行还是中止当前的任务。
 * </p>
 */
public class ProgressPool {
    
	/**
	 * 执行中的进度条池
	 */
	static Map<String, Progress> progressMap = new HashMap<String, Progress>();
	static Pool shortCache = CacheHelper.getShortCache();
	
	/**
	 * 放入进度条对象
	 */
	public static void putSchedule(String code, Progress obj){
		shortCache.putObject(getClient(), new Object());
		progressMap.put(code, obj);
	}
	
	public static Progress getSchedule(String code){
		return progressMap.get(code);
	}
	
	public static Progress removeSchedule(String code){
		CacheHelper.getShortCache().destroyByKey( getClient() );
		return progressMap.remove(code);
	}
	
	public static void finish(Progress progress) {
		progress.add(9999999); // 进度
		CacheHelper.getShortCache().destroyByKey( getClient() );
	}
	
	public static void checkRepeat() {
		Cacheable item = shortCache.getObject( getClient() );
		if( item != null && System.currentTimeMillis() - item.getBirthday() < 3*60*1000 ) {
			throw new BusinessException(EX.CACHE_6);
		}
	}
	
	private static String getClient() {
		return "pg-" + Environment.getUserId() + "-" + Environment.getClientIp();
	}
}
