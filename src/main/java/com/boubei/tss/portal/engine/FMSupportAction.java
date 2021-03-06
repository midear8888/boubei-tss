/* ==================================================================   
 * Created [2009-09-27] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.portal.engine;

import java.io.File;
import java.net.URL;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.framework.web.mvc.BaseActionSupport;
import com.boubei.tss.portal.PortalConstants;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.URLUtil;

/** 
 * 支持freemarker引擎解析的Action基类。
 * 
 * 可用于组件的预览等功能。
 * 
 */
public abstract class FMSupportAction extends BaseActionSupport {

	protected Logger log = Logger.getLogger(this.getClass());
 
    /**
     * 解析Freemarker模板
     */
	
    protected void printHTML(String template){
        printHTML(null, template);
    }
    
    protected void printHTML(Long portalId, String template){
        printHTML(portalId, template, true);
    }
    
    protected void printHTML(Long portalId, String template, boolean parseTwice){
        try {
            FreemarkerParser parser = getFreemarkerParser(portalId);
            HttpServletResponse response = Context.getResponse();
            response.setContentType("text/html;charset=UTF-8");
            if(parseTwice) { // 是否需要解析两次
                parser.parseTemplateTwice(template, response.getWriter());
            } else {
                parser.parseTemplate(template, response.getWriter());
            }
            return;
        } 
        catch (Exception e) {
            String errorInfo = "出错了,请联系管理员！<br/>错误信息: " + e.getMessage() + "<br/>";
            print(errorInfo);
            return;
        } 
    }
    
	protected FreemarkerParser getFreemarkerParser(Long portalId) {
        HttpServletRequest  request = Context.getRequestContext().getRequest();
        
        // 设置门户资源路进为Freemarker模板路径
        File ftl = getPortalResourcesPath(portalId);
		FreemarkerParser parser = new FreemarkerParser(ftl);
		
        /* 
         * 将本次http请求中带的参数放入到freemarker数据模型中，以方便调用，
         * 因为是每次请求都新建一个FreemarkerParser，所以不会存在多线程问题。
         */
        parser.putParameters(request.getParameterMap()); 
        parser.putParameters(DMUtil.getUserDefineParams()); 
        
        return parser;
    }
    
    protected File getPortalResourcesPath(Long portalId) {
        if(portalId == null) return null;
        
        URL url = URLUtil.getWebFileUrl(PortalConstants.PORTAL_MODEL_DIR);
        List<File> dirList = FileHelper.listSubDir(new File(url.getFile()));
        for( File dir : dirList ) {
            // 判断文件夹路径是否以 "_12"这样结尾，是的话表示是当前门户的资源文件夹
            if(dir.getName().endsWith("_" + portalId)) {
                return dir;
            }
        }
        return null;
    }

}

