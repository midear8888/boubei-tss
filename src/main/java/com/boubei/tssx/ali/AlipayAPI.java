package com.boubei.tssx.ali;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.util.EasyUtils;

@Controller
@RequestMapping("/alipay/api")
public class AlipayAPI {
 
	/**
	 * PC场景下单并支付
	 * appid 应用编号
	 * out_trade_no 商户订单号
	 * product_code 销售产品码
	 * total_amount 订单总金额，单位为元
	 * subject 订单标题
	 * body 订单描述
	 * http://127.0.0.1:9000/tss/alipay/api/pagepay?appid=2018051160132356&out_trade_no=1000000&product_code=FAST_INSTANT_TRADE_PAY&total_amount=1&subject=1234&body=1234
	 * https://www.boudata.com/tss/alipay/api/pagepay?appid=2018051160132356&out_trade_no=1000005&product_code=FAST_INSTANT_TRADE_PAY&total_amount=0.01&subject=%E6%94%AF%E4%BB%98&body=%E6%94%AF%E4%BB%98%E5%86%85%E5%AE%B9
	 */
	@RequestMapping(value = "/pagepay")
	@ResponseBody
	public void pagepay(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		Map<String, String> requestMap = DMUtil.parseRequestParams(request, false);
		AlipayConfig alipay = new AlipayConfig( requestMap.remove("appid") );
		
		AlipayClient aClient = new DefaultAlipayClient(alipay.getUrl(), alipay.appid, alipay.getPrivateKey(), "json", AlipayConfig.Char_Set, alipay.getAlipayKey(), AlipayConfig.Sign_Type); //获得初始化的AlipayClient
	    AlipayTradePagePayRequest aRequest = new AlipayTradePagePayRequest(); // 创建API对应的request
	    aRequest.setNotifyUrl(alipay.getNotifyUrl()); // 在公共参数中设置回跳和通知地址
	    
	    String bizcontent = EasyUtils.obj2Json(requestMap);
	    aRequest.setBizContent(bizcontent); // 填充业务参数
 
	    String form = aClient.pageExecute(aRequest).getBody(); // 调用SDK生成表单
	    
	    response.setContentType("text/html;charset=" + AlipayConfig.Char_Set);
	    response.getWriter().write(form); // 直接将完整的表单html输出到页面
	    response.getWriter().flush();
	    response.getWriter().close();
	}
	
	/**
	 * 下单查询
	 * appid 应用编号
	 * out_trade_no 商户订单号
	 * @throws IOException
	 */
	@RequestMapping(value = "/query")
	@ResponseBody
	public void query(HttpServletRequest request, HttpServletResponse response) throws AlipayApiException, IOException {
		
        Map<String, String> requestMap = DMUtil.parseRequestParams(request, false);
		AlipayConfig alipay = new AlipayConfig( requestMap.remove("appid") );
		
		AlipayClient aClient = new DefaultAlipayClient(alipay.getUrl(), alipay.appid, alipay.getPrivateKey(),
				"json", AlipayConfig.Char_Set, alipay.getAlipayKey(), AlipayConfig.Sign_Type); // 获得初始化的AlipayClient
	    AlipayTradeQueryRequest aRequest = new AlipayTradeQueryRequest(); // 创建API对应的request
	    
        String bizcontent = EasyUtils.obj2Json(requestMap);
	    aRequest.setBizContent(bizcontent);
	    
	    try {
			AlipayTradeQueryResponse ret = aClient.execute(aRequest);
			response.setContentType("text/plain;charset=UTF-8");
			
			@SuppressWarnings("unchecked")
			Map<Object, Object> map = new ObjectMapper().readValue(ret.getBody(), Map.class);
			
			@SuppressWarnings("unchecked")
			Map<Object, Object> trade_map = (Map<Object, Object>) map.get("alipay_trade_query_response");
			
			if("10000".equals(trade_map.get("code")) && "TRADE_SUCCESS".equals(trade_map.get("trade_status"))){
				response.getWriter().println("{\"code\": \"success\", \"data\": \"支付成功\"}");
			}
			else{
				response.getWriter().println("{\"code\": \"fail\", \"errorMsg\": \"" + trade_map.get("msg") + "\"}");
			}
			
		} catch (AlipayApiException e) {
			throw new BusinessException(e.getMessage(), e);
		}
	}
}

/*{
	"alipay_trade_query_response": {
		"code": "10000",
		"msg": "Success",
		"buyer_logon_id": "150****1905",
		"buyer_pay_amount": "0.00",
		"buyer_user_id": "2088702817195730",
		"invoice_amount": "0.00",
		"out_trade_no": "1000001",
		"point_amount": "0.00",
		"receipt_amount": "0.00",
		"send_pay_date": "2018-06-06 17:02:38",
		"total_amount": "1.00",
		"trade_no": "2018060621001004730530027423",
		"trade_status": "TRADE_SUCCESS"
	},
	"sign": "OmJx1aun8rLh08teqehCmNLiVQPugs4DAps8lbPkkEQB4duvZLndntGM5WdyAR74l/UjNTFBEif7pJM4pyAqSANoSKj/4cLX00EUeNasDyFTMh1mZGoaq7cTVyCCyunc6PasAxdXE6FzkGcevC2h0n5OOdFcVJ2I+S5WIw6EFqabjmKf8NxBkYYvm7mAutUb2VCTMGDobdK9+RzIsxFUWpCpy1WjRnPO90FneBVQIL0+vlpgKYnxhp+uU9tB5S10uJfCkEe0zQiASxCikGbnNKmE+R94WcwBs4bFp52aszH1zQWqSkxSb7Q5fHbj6WkdRYc7VaTsdE0o68YvogvxhQ=="
}*/
/*{
	"alipay_trade_query_response": {
		"code": "40004",
		"msg": "Business Failed",
		"sub_code": "ACQ.TRADE_NOT_EXIST",
		"sub_msg": "交易不存在",
		"buyer_pay_amount": "0.00",
		"invoice_amount": "0.00",
		"out_trade_no": "1000002",
		"point_amount": "0.00",
		"receipt_amount": "0.00"
	},
	"sign": "AfU0fi2T5WzcYJpHDgtmp4eEQGE8Fqy5lu8RLctCDyYZH2BOTokrWBnLAjoecZjX29dFspUnccIY/XfSoCoiVlQ14TsRnYEIcb1hE8FMaqZuvdbS0CPTQLC88DslIzfYvCunQHbX/WqbEgdsMVPNbq6/XiLiiDuBOmfu5QZbvUH0reR6jyqjtEFZHadMoUzwKKaAhhEe04jT8ltP6jWIs8NZS7bSLpHQ2MDh7hd4GOjfuyBRbUYxx64CaB42OUXk+6PpJ7rYKrvFOB4pbXNAuLFill73bpTvDTtHhp9jfTDxt81tSGEWHZz/4clm/Jwo0SUcDDG0xz8PnYuRpvxWNw=="
}*/
