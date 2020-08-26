package com.boubei.tss.cache.aop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.framework.persistence.IEntity;
import com.boubei.tss.modules.param.ParamConfig;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;

public class CacheListener {
	/**
	 * SHORT|functionName(param1,
	 * 支持以本条数据的值解析
	 * eg {domain:D006, order_id:9},
	 * 则配置SHORT|getOrderCode(${order_id},  会先解析成getOrderCode(9,  再去清除缓存
	 * 支持系统参数配置清除缓存，如实体为DomainConfig，则配置DomainConfigCacheClear的内容（多个用#分隔）也会被读取
	 */
	public static void afterEntityModify(IEntity e) {
		CacheClear cacheClear = e.getClass().getAnnotation(CacheClear.class);
		if (cacheClear == null)
			return;

		List<String> list = new ArrayList<>( Arrays.asList(cacheClear.values()) );

		// read sys setting
		String param = ParamConfig.getAttribute(e.getClass().getSimpleName() + "CacheClear", "");
		list.addAll( Arrays.asList(param.split("#")) );

		for (String cache : list) {
			if (EasyUtils.isNullOrEmpty(cache))
				continue;

			String[] _cache = cache.split("\\|");
			String poolName = _cache[0];
			String likeKey  = _cache[1];
			if (likeKey.indexOf("$") > -1) {
				likeKey = EasyUtils.fmParse(likeKey, BeanUtil.getProperties(e));
			}
			CacheHelper.flushCache(poolName, likeKey);
		}
	}
}
