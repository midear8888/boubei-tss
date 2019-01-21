package com.boubei.tssx.wx;

import java.io.IOException;
import java.util.Map;

import com.boubei.tss.um.entity.User;

public interface WxService {

	User getUserByAuthToken(String authToken);

	User checkPhoneNum(String phoneNum);

	String regWxUser(User user, String domain, String groupName);

	/**
	 * 绑定微信openId到同手机号用户
	 */
	void bindOpenID(User user, String openID);
	
	User getBelongUser(String belong);
	
	/**
     * 公众号发送模板消息
     * appid 公众号ID 用于获取 access_token
     * phone 接收者（已关注公众号且绑定手机号）的phone 关联获取 openid ，13735547815
     * touser 接收者（用户）的 openid ，oNi3W5XOKp6GoFtHoX3iKa5gxyRg
     * template_id 所需下发的模板消息的id ，K1uGpcq0oLh3tdYPle5Ciemjlg3KCDMu9YG51jW9_S4
     * url 模板跳转链接（海外帐号没有跳转能力），https://www.boudata.com/tss/pages/gzh.html
     * miniprogram 跳小程序所需数据，不需跳小程序可不用传该数据 ，{\"appid\": \"wx5255074da90a4dca\",\"pagepath\": \"pages/homepage/index\"}
     * data 模板内容 ，{\"first\":{\"color\":\"#173177\",\"value\":\"您的订单已提交成功\"},\"keyword1\":{\"value\":\"CX31806300002\"},...}
     * http://127.0.0.1:9000/tss/wx/api/sendgzhmsg?上述参数
     * 
     * 小程序端调用方式
     * tss.sendGZHMsg({
	      params: {
	        appid: 公众号ID （卜数科技：'wx784c62545bddf62b'）, // 必填
	        phone: 接收者（已关注公众号且绑定手机号）的phone, // 必填
	        template_id: 模版ID 前往公众平台获取, // 必填
	        data: 模板内容, // 必填
	        url: 模板跳转链接, // 选填
	        miniprogram: { appid: 小程序ID, pagepath: 小程序对应页面 } 跳小程序所需数据 // 选填
	      },
	      completion: res => {
	        console.log(res)
	      }
	    })
     * 实例：
     * data = {
	      first: { value: '您的订单已提交成功', color: '#173177'},
	      keyword1: { value: 'CX31806300002' },
	      keyword2: { value: '2019-01-08 11:40:00' },
	      keyword3: { value: '16吨书籍' },
	      keyword4: { value: '姓名：张三 电话：13800138000' },
	      keyword5: { value: '车牌：粤B88888 车型：厢式车' },
	      remark: { value: '点击查看详情。客服电话：400-888-8888' }
	    }, 
    * miniprogram = {
	      appid: 'wx5255074da90a4dca',
	      pagepath: 'pages/homepage/index'
	    }
    * tss.sendGZHMsg({
	      params: {
	        appid: 'wx784c62545bddf62b',
	        phone: '13735547815',
	        template_id: 'K1uGpcq0oLh3tdYPle5Ciemjlg3KCDMu9YG51jW9_S4',
	        miniprogram: miniprogram,
	        data: data
	      },
	      completion: res => {
	        console.log(res)
	      }
	    })
	 * @throws IOException 
     */
	String sendWxGZHMsg(Map<String, String> requestMap) throws IOException;
	
	String sendWxGZHMsgAll(Map<String, String> requestMap) throws IOException;

}
