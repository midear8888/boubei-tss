/* ==================================================================   
 * Created [2009-7-7] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.sso;


import com.boubei.tss.modules.param.ParamConfig;
import com.boubei.tss.util.EasyUtils;

/**
 * <p>
 * 登录自定义器工厂
 * </p>
 */
public class LoginCustomizerFactory {

    /** 自定义器实体 */
    public static ILoginCustomizer customizer;

    /** 工厂类实体 */
    private static LoginCustomizerFactory factory;

    /**
     * <p>
     * 获取自定义器
     * </p>
     * @return ILoginCustomizer 自定义器
     */
    public ILoginCustomizer getCustomizer() {
        if (customizer == null) {
            String className = ParamConfig.getAttribute(SSOConstants.LOGIN_COSTOMIZER);
            className = (String) EasyUtils.checkNull(className, DoNothingLoginCustomizer.class.getName());
            String[] classNames = className.split(",");
            customizer = new ArrayLoginCustomizer(classNames);
        }
        return customizer;
    }

    /**
     * <p>
     * 实例化工厂类
     * </p>
     * @return
     */
    public static LoginCustomizerFactory instance() {
        if (factory == null) {
            factory = new LoginCustomizerFactory();
        }
        return factory;
    }
    
    public static void destroy() {
    	factory = null;
    	customizer = null;
    }
}
