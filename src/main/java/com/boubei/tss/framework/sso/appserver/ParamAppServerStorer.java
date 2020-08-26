/* ==================================================================   
 * Created [2009-7-7] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.sso.appserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.dom4j.Element;

import com.boubei.tss.EX;
import com.boubei.tss.cache.Cacheable;
import com.boubei.tss.cache.Pool;
import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.modules.param.ParamManager;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.XMLDocUtil;

/** 
 * appServer管理启用参数管理(ParamAppServerStorer)，以免增加应用就要修改appServer.xml文件。
 * 
 */
public class ParamAppServerStorer implements IAppServerStorer {
	
	Pool cache = CacheHelper.getNoDeadCache();
	String prefix = "APP-";
    
    public AppServer getAppServer(String appCode) {
		String cacheKey = prefix + appCode;
    	Cacheable item = cache.getObject(cacheKey);
		if(item != null) {
    		return (AppServer) item.getValue(); // 修改AppServer配置后需刷新 nodead cache
    	}
    	
        String appServerXML = null;
        try {
        	// 读取appServer配置不依赖Spring容器，因某些bean初始化本身就要用到AppServer
        	appServerXML = ParamManager.getValueNoSpring(appCode);
        }
        catch(Exception e) { }
        
        if(appServerXML == null) {
            throw new BusinessException( EX.parse(EX.F_06, appCode) ); 
        }
        
        Element appServerNode = XMLDocUtil.dataXml2Doc(appServerXML).getRootElement();
        Map<String, String> attrsMap = XMLDocUtil.dataNode2Map(appServerNode);

        AppServer bean = new AppServer();
        BeanUtil.setDataToBean(bean, attrsMap);
        
        cache.putObject(cacheKey, bean);
        return bean;
    }
    
	public Collection<AppServer> getAppServers() {
		Collection<AppServer> result = new ArrayList<>();
		Set<Cacheable> items = cache.listItems();
		for(Cacheable item : items) {
			if( item.getKey().toString().startsWith(prefix)) {
				result.add( (AppServer) item.getValue() );
			}
		}
		
		return result;
	}
}

