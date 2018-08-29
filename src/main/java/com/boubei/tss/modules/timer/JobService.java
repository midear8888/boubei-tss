package com.boubei.tss.modules.timer;

import com.boubei.tss.cache.aop.Cached;
import com.boubei.tss.cache.aop.QueryCached;
import com.boubei.tss.cache.extension.CacheLife;

public interface JobService {
	
	/**
	 * 如果相同Job已在手动执行，则不再重复执行。
	 * 三分钟内只能手动执行一次
	 */
	@QueryCached
	@Cached(cyclelife = CacheLife.SHORTER)
	String excuteJob(String jobKey, Object tag);
	
	/**
	 * 如果相同Task已在执行，则不再重复执行。
	 * 三分钟内一个人只能手动执行一次
	 */
	@QueryCached
	@Cached(cyclelife = CacheLife.SHORTER)
	String excuteTask(Long taskId, Object tag);

}
