package com.boubei.tss.framework.sso;

import java.util.ArrayList;
import java.util.List;

import com.boubei.tss.util.BeanUtil;

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
    }
 
    public void execute() {
        if (customizers == null) {
            init();
        }

        for(ILoginCustomizer customizer : customizers) {
        	customizer.execute();
        }
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