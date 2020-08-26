package com.boubei.tss.framework.sso;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.framework.sso.context.RequestContext;
import com.boubei.tss.framework.web.HttpClientUtil;
import com.boubei.tss.um.action.UserAction;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.InfoEncoder;

/** 
 * <p>
 * 登录时参数自定义器：
 * 可以同时执行多个定义的登录时参数自定义器，定义方法为使用逗号格开多个自定义器全类名
 * </p>
 */
public class ArrayLoginCustomizer implements ILoginCustomizer {

    /**
     * 自定义器列表
     */
    private List<ILoginCustomizer> customizers;

    /**
     * 自定义器类名数组
     */
    private String[] classNames;
 
    public ArrayLoginCustomizer(String[] classNames) {
        this.classNames = classNames;
        this.init();
    }
 
    public void execute() {
        for(ILoginCustomizer customizer : customizers) {
        	customizer.execute();
        }
        
        /* 保存userHas到客户端Cookie */
		Object[] userHas = UserAction.getUserHas();
		String encodeVal = InfoEncoder.simpleEncode(EasyUtils.obj2Json(userHas), 12);
		
		HttpServletResponse response = Context.getResponse();
		HttpClientUtil.setCookie(response, RequestContext.USER_HAS, encodeVal);
		HttpClientUtil.setCookie(response, RequestContext.SERVER_TIME, DateUtil.format( new Date() ) );
    }

    /**
     * <p>
     * 初始化自定义器列表
     * </p>
     */
    private void init() {
        customizers = new ArrayList<ILoginCustomizer>();
	    for (String className : classNames) {
            Object customizer = BeanUtil.newInstanceByName(className);
            customizers.add( (ILoginCustomizer) customizer );
        }
    }
}