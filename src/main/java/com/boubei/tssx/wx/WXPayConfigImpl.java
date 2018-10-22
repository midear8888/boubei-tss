package com.boubei.tssx.wx;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import com.boubei.tss.modules.param.ParamConfig;
import com.boubei.tss.util.URLUtil;
import com.github.wxpay.sdk.WXPayConfig;

public class WXPayConfigImpl implements WXPayConfig{

    private byte[] certData;
    private String appid;
    private String mchid;

    public WXPayConfigImpl(String appid, String mchid) throws Exception {
    	this.appid = appid;
    	this.mchid = mchid;
        String certPath = URLUtil.getClassesPath().getPath() + "/cert/" + mchid + ".p12";
        File file = new File(certPath);
        InputStream certStream = new FileInputStream(file);
        this.certData = new byte[(int) file.length()];
        certStream.read(this.certData);
        certStream.close();
    }

    public String getAppID() {
    	return appid; 
    }

    public String getMchID() {
    	return mchid;
    }

    public String getKey() {
    	return ParamConfig.getAttribute(mchid, ""); 
    }

    public InputStream getCertStream() {
        return new ByteArrayInputStream(this.certData);
    }

    public int getHttpConnectTimeoutMs() {
        return 8000;
    }

    public int getHttpReadTimeoutMs() {
        return 10000;
    }

}
