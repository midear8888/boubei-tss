package com.boubei.tssx.wx;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.DataExport;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.dm.record.permission.RecordPermission;
import com.boubei.tss.dm.record.permission.RecordResource;
import com.boubei.tss.dm.record.workflow.WFService;
import com.boubei.tss.dm.record.workflow.WFUtil;
import com.boubei.tss.dm.report.Report;
import com.boubei.tss.dm.report.ReportService;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.modules.param.ParamConfig;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.modules.timer.JobService;
import com.boubei.tss.um.permission.PermissionHelper;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;
import com.boubei.tssx.ImageCodeAPI;
import com.github.wxpay.sdk.WXPay;
import com.github.wxpay.sdk.WXPayUtil;

@Controller
@RequestMapping({"/wx/api", "/auth/wx/api"})
public class WxAPI {
	
	Logger log = Logger.getLogger(this.getClass());
	
	@Autowired RecordService recordService;
	@Autowired ReportService reportService;
	@Autowired WFService wfService;
	@Autowired ILoginService loginService; 
	@Autowired JobService jobService;
	@Autowired WxService wxService;
	
	/**
	 * http://localhost:9000/tss/wx/api/roles
	 * 
	 * 系统角色列表，用于解析角色名对角色ID。 
	 * 例：staff_info.position记录的是角色ID，判断是否包含某角色时不宜写死角色ID，通过名称先去找出角色ID
	 * {"开发":82,"QA":76,"下单客户":2,"业务员":73,"人事经理":69....}
	 */
	@RequestMapping(value = "/roles")
	@ResponseBody
	public Map<Object, Object> getRoles() throws Exception {
		String sql = "select id, name from um_role where isGroup=0 and disabled=0 and id > 0 ";
		List<Map<String, Object>> list = SQLExcutor.queryL(sql);
		Map<Object, Object> roleMap = new HashMap<Object, Object>();
		for( Map<String, Object> m : list ) {
			Object roleName = m.get("name");
			Object roleId = m.get("id");
			
			roleMap.put(roleName, roleId);
			roleMap.put(roleId, roleName);
		}
		return roleMap;
	}
	
	/**
	 * http://localhost:9000/tss/wx/api/users
	 * 登陆账号和中文名字映射
	 */
	@RequestMapping(value = "/users")
	@ResponseBody
	public Map<String, String> getUsers() {
		return loginService.getUsersMap();
	}
	
	@RequestMapping(value = "/users_ex_customer")
	@ResponseBody
	public Map<String, String> getUsersExcludeCustomer() {
		String field = "loginName, u.userName, g.name groupName";
		List<?> list = loginService.getUsersByDomain(Environment.getDomain(), field, Environment.getUserId());
		
		Map<String, String> map = new HashMap<String, String>();
		for( Object obj : list ) {
			Object[] objs = (Object[]) obj;
			boolean userInCustomer = "customer".equals(objs[2]);
			if( !userInCustomer ) {
				map.put((String)objs[0], (String)objs[1]);
			}
		}
		return map;
	}
	
	@RequestMapping(value = "/users/id2name")
	@ResponseBody
	public Map<Long, String> getUsersI() {
		return loginService.getUsersMapI();
	}
	
	/**
	 * http://localhost:9000/tss/wx/api/myroles
	 * 获取用户个人所有用的角色
	 */
	@RequestMapping(value = "/myroles")
	@ResponseBody
	public Object getMyRoles() {
		HttpSession session = Context.getRequestContext().getSession();
		Object roles = session.getAttribute(SSOConstants.USER_ROLES_S);
		return roles;
	}
	
	/**
	 * POST: /wx/api/user/name ? userName=xxx
	 */
	@RequestMapping(value = "/user/name", method = RequestMethod.POST)
	@ResponseBody
	public Object updateUserName(HttpServletRequest request) {
		Map<String, String> requestMap = DMUtil.parseRequestParams(request, true);
		String userName = requestMap.get("userName");
		
		String sql= "update um_user set userName = '" +userName+ "' where id = " +Environment.getUserId(); 
		SQLExcutor.excute(sql, DMConstants.LOCAL_CONN_POOL);
		
		return "success";
	}
	
	/**
	 * 小程序码：https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=ACCESS_TOKEN
	 * 
	 * eg: http://localhost:9000/tss/wx/api/acode2?appId=wxbc6dbb3ffae1fafa    业务员（商家）定制版
	 *     http://localhost:9000/tss/wx/api/acode2?appId=wxbc6dbb3ffae1fafa&domain=0  卜数定制版（适合商家邀请商家）
	 *     https://www.boudata.com/tss/wx/api/acode2?appId=wx5255074da90a4dca&page=pages/homepage/index 卜数首页
	 */
	@RequestMapping(value = "/acode2")
	@ResponseBody
	public void getWxacodeunlimit(HttpServletRequest request, HttpServletResponse response, String appId) throws Exception {
		String access_token = new WXUtil().getToken(appId);
		String url = "https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=" + access_token;
		
		String scene = getDomain(request) + ",customer," + Environment.getUserCode();
		String qparam = EasyUtils.obj2String(request.getParameter("qparam"));
		if( !EasyUtils.isNullOrEmpty(qparam) ) {
			scene += "," + qparam;
		}
		
		JSONObject json = new JSONObject();
		json.put("scene", scene); // "CX,customer,13588833834"  不支持中文
		json.put("width", 430);
		
		String page = request.getParameter("page");
		if( !EasyUtils.isNullOrEmpty(page) ) {
			json.put("page", page); // 默认跳转页
		}
		
		createWxCode(response, appId, url, json);
	}
	
	/**
	 * 小程序码：https://api.weixin.qq.com/wxa/getwxacode?access_token=ACCESS_TOKEN
	 * 普二维码：https://api.weixin.qq.com/cgi-bin/wxaapp/createwxaqrcode?access_token=ACCESS_TOKEN
	 * 
	 * eg: http://localhost:9000/tss/wx/api/acode?appId=wxbc6dbb3ffae1fafa&qrCode=true
	 *     https://www.boudata.com/tss/wx/api/acode?appId=wx5255074da90a4dca&page=pages/homepage/index
	 */
	@RequestMapping(value = "/acode")
	@ResponseBody
	public void getWxacode(HttpServletRequest request, HttpServletResponse response, String appId, boolean qrCode) throws Exception {
		String access_token = new WXUtil().getToken(appId);
		String url;
		if( qrCode ) {
			 url = "https://api.weixin.qq.com/cgi-bin/wxaapp/createwxaqrcode";
		} else {
			url = "https://api.weixin.qq.com/wxa/getwxacode";
		}
		url += "?access_token=" + access_token;
		
		String param = "domain=" +getDomain(request)+ "&group=customer&belong=" + Environment.getUserCode();
		String qparam = EasyUtils.obj2String(request.getParameter("qparam"));
		if( !EasyUtils.isNullOrEmpty(qparam) ) {
			param += "&qparam=" + qparam;
		}
		
		String page = request.getParameter("page");
		page = (String) EasyUtils.checkNull(page, "pages/homepage/homepage");
		
		JSONObject json = new JSONObject();
		json.put("path", page + "?" +param);
		json.put("width", 430);
		
		createWxCode(response, appId, url, json);
	}
	
	// domain="0": 是【商家】注册二维码；否则是【客户】注册二维码
	protected String getDomain(HttpServletRequest request) {
		Map<String, String> requestMap = DMUtil.parseRequestParams(request, false);
		String domain = requestMap.get("domain");
		return EasyUtils.obj2String( "0".equals(domain) ? domain : Environment.getDomainOrign() );  // null --> 空字符串
	}
	
	protected void createWxCode(HttpServletResponse response, String appId, String url, JSONObject json) throws Exception {
		byte[] b = json.toString().getBytes("UTF-8");
        InputStream is = new ByteArrayInputStream(b, 0, b.length);
		RequestEntity re = new InputStreamRequestEntity(is, b.length, "application/json; charset=UTF-8"); // json里不支持中文
		
		PostMethod postMethod = new PostMethod(url);
		postMethod.setRequestHeader("Content-Type", "application/json");
		postMethod.setRequestEntity(re);
		
		HttpClient httpClient = new HttpClient();
		httpClient.executeMethod(postMethod);   // log.debug( postMethod.getResponseBodyAsString() );
		
		String imgName = appId + "_Q.png";
		String imgPath = DataExport.getExportPath() + "/" + imgName;
		WXUtil.save2Image( postMethod.getResponseBodyAsStream(), imgPath );
		FileHelper.downloadFile(response, imgPath, imgName, true);
	}
	
	@RequestMapping("/wftables")
    @ResponseBody
    public List<Map<String, Object>> getWfRecords4WX(HttpServletResponse response) {
    	List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
    	
    	List<Record> list = recordService.getRecordables();
    	Map<Object, Object> countMap = wfService.getMyWFCount();
    	for(Record record : list) {
    		boolean isWFRecord = WFUtil.checkWorkFlow(record.getWorkflow());
    		if(!record.isActive() || !isWFRecord ) continue;
    		
    		String wxicon = DMUtil.getExtendAttr(record.getRemark(), "wxicon");
    		if( wxicon == null && !ParamConstants.TRUE.equals(record.getMobilable()) ) continue;
    		
    		Map<String, Object> item = new HashMap<String, Object>();
    		Long id = record.getId();
			item.put("id", id);
    		item.put("pid", record.getParentId());
    		item.put("name", record.getName());
    		item.put("table", record.getTable());
			item.put("wxicon", EasyUtils.checkNull(record.getWxicon(), "/tss/images/wf.png"));
			item.put("wxurl", record.getWxurl());
			item.put("wfingCount", countMap.get(id));
    		
			result.add( item );
    	}
    	
    	return result;
    }
	
	@RequestMapping("/rctables")
    @ResponseBody
    public List<Map<String, Object>> getRecords4WX(HttpServletResponse response) {
    	List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
    	
    	PermissionHelper ph = PermissionHelper.getInstance();
    	Set<Record> set = new LinkedHashSet<>();
    	set.addAll(recordService.getRecordables());
    	set.addAll(recordService.getVisiables());
    	for(Record record : set) {
    		boolean isWFRecord = WFUtil.checkWorkFlow(record.getWorkflow());
    		if( !ParamConstants.TRUE.equals(record.getMobilable()) || !record.isActive() || isWFRecord ) continue;
    		
    		Map<String, Object> item = new HashMap<String, Object>();
    		Long id = record.getId();
			item.put("id", id);
    		item.put("pid", record.getParentId());
    		item.put("name", record.getName());
    		item.put("table", record.getTable());
			item.put("wxicon", EasyUtils.checkNull(record.getWxicon(), "/tss/images/record.png"));
			item.put("icon", item.get("wxicon"));
			item.put("wxurl", record.getWxurl());
			item.put("permissions", ph.getOperationsByResource(id, RecordPermission.class.getName(), RecordResource.class));
    		
			result.add( item );
    	}
    	
    	return result;
    }
	
	@RequestMapping("/wxreports")
    @ResponseBody
    public List<Map<String, Object>> getReports4WX(HttpServletResponse response) {
    	List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
    	
    	List<Report> list = reportService.getAllReport();
    	for(Report report : list) {
    		if( !report.isActive() || report.isGroup() 
    				|| !ParamConstants.TRUE.equals(report.getMobilable()) ) {
    			continue;
    		}
    		
    		Map<String, Object> item = new HashMap<String, Object>();
    		Long id = report.getId();
			item.put("id", id);
    		item.put("name", report.getName());
    		item.put("code", report.getCode());
    		item.put("wxurl", DMUtil.getExtendAttr(report.getRemark(), "wxurl"));
    		String wxicon = DMUtil.getExtendAttr(report.getRemark(), "wxicon");
			item.put("wxicon", EasyUtils.checkNull(wxicon, "/tss/images/report.png"));
			item.put("colDefs", report.getColDefs());
    		
			result.add( item );
    	}
    	
    	return result;
    }
	
	@RequestMapping(value = "/job/{key}", method = RequestMethod.POST)
	@ResponseBody
	public Object exucteJob(@PathVariable String key) {
		return jobService.excuteJob(key, System.currentTimeMillis()); // 总是忽略QueryCache缓存
	}
		
	private WXPay getWXPay(Map<String, String> requestMap) throws Exception {
		String appid = requestMap.remove("appid");
		String mchid = requestMap.remove("mchid");
		
	    WXPayConfigImpl config = new WXPayConfigImpl(appid, mchid);
	    WXPay wxpay = new WXPay(config);
		return wxpay;
	}
    
	/**
	 * 扫码支付
	 * body 商品描述
	 * out_trade_no 商户订单号
	 * total_fee 标价金额
	 * product_id 商品ID
	 * appid 小程序app ID
	 * mchid 商户ID
	 * http://127.0.0.1:9000/tss/wx/api/scanpay?body=支付测试&out_trade_no=8001&total_fee=1&product_id=8001&appid=wx32ecfcea6f822096&mchid=1503974521
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/scanpay")
	@ResponseBody
	public void scanPay(HttpServletRequest request, HttpServletResponse response) throws Exception{
		Map<String, String> requestMap = DMUtil.parseRequestParams(request, false);
		
		String ip = request.getRemoteAddr(); 
		
		HashMap<String, String> data = new HashMap<String, String>();
        data.put("body", requestMap.get("body"));
        data.put("out_trade_no", requestMap.get("out_trade_no"));
        data.put("total_fee", requestMap.get("total_fee"));
        data.put("spbill_create_ip", ip);
        data.put("trade_type", "NATIVE");
        data.put("product_id", requestMap.get("product_id"));
        
        data.put("appid", requestMap.get("appid"));
        data.put("mchid", requestMap.get("mchid"));
        
        response.setContentType("text/plain;charset=UTF-8");
        
        Map<String, Object> result  = unifiedOrder(data);
        
        if(result.get("code").equals("success")){
			Map<String, String> ret = (Map<String, String>) result.get("data");
        	new ImageCodeAPI().createQrBarCodeImg(ret.get("code_url"), request, response);
        }
        else{
        	response.getWriter().println("{\"code\": \"fail\", \"errorMsg\": \"" + result.get("errorMsg") + "\"}");
        }
	}
	
	/**
	 * 小程序支付
	 * body 商品描述
	 * out_trade_no 商户订单号
	 * total_fee 标价金额
	 * openid 小程序账号唯一标识
	 * appid 小程序app ID
	 * mchid 商户ID
	 * http://127.0.0.1:9000/tss/wx/api/minipay?body=支付测试&out_trade_no=8002&total_fee=1&openid=oNi3W5XOKp6GoFtHoX3iKa5gxyRg&appid=wx32ecfcea6f822096&mchid=1503974521
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/minipay")
	@ResponseBody
	public void miniProgramPay(HttpServletRequest request, HttpServletResponse response) throws Exception{
		Map<String, String> requestMap = DMUtil.parseRequestParams(request, false);
		
		String appid = requestMap.get("appid");
		String mchid = requestMap.get("mchid");
		
		String ip = request.getRemoteAddr();  
		
		HashMap<String, String> data = new HashMap<String, String>();
        data.put("body", requestMap.get("body"));
        data.put("out_trade_no", requestMap.get("out_trade_no"));
        data.put("total_fee", requestMap.get("total_fee"));
        data.put("spbill_create_ip", ip);
        data.put("trade_type", "JSAPI");
        data.put("openid", requestMap.get("openid"));
        
        data.put("appid", appid);
        data.put("mchid", mchid);
        
        WXPayConfigImpl config = new WXPayConfigImpl(appid, mchid);
        String key = config.getKey();
        
        response.setContentType("text/plain;charset=UTF-8");
        
        Map<String, Object> result  = unifiedOrder(data);
        
        if(result.get("code").equals("success")){
        	Map<String, String> ret = (Map<String, String>) result.get("data");
        	String nonceStr = ret.get("nonce_str");
        	String packAge = "prepay_id=" + ret.get("prepay_id");
        	String timeStamp = EasyUtils.obj2String(new Date().getTime());
        	
        	Map<String, String> signMap = new HashMap<String, String>();
        	signMap.put("appId", appid);
        	signMap.put("nonceStr", nonceStr);
        	signMap.put("package", packAge);
        	signMap.put("signType", "MD5");
        	signMap.put("timeStamp", timeStamp);
        	String sign = WXPayUtil.generateSignature(signMap, key);
        	
        	JSONObject r = new JSONObject();
        	r.put("nonceStr", nonceStr);
        	r.put("package", packAge);
        	r.put("signType", "MD5");
        	r.put("timeStamp", timeStamp);
        	r.put("paySign", sign);
        	
        	response.getWriter().println("{\"code\": \"success\", \"data\": " + r.toString() + "}");
        }
        else{
        	response.getWriter().println("{\"code\": \"fail\", \"errorMsg\": \"" + result.get("errorMsg") + "\"}");
        }
	}
	
	/**
	 * 统一下单
	 */
	public Map<String, Object> unifiedOrder(Map<String, String> data) throws Exception {
		String domain = ParamConfig.getAttribute("notify_Url", "");
		String notify_url = "https://"+ domain + "/tss/wxnotify.in";
		data.put("notify_url", notify_url);
		
		WXPay wxpay = getWXPay(data);
		Map<String, String> r = wxpay.unifiedOrder(data);
		
		Map<String, Object> result = new HashMap<String, Object>();
        
    	if( "SUCCESS".equals(r.get("return_code")) && "SUCCESS".equals(r.get("result_code")) ) {
    		result.put("code", "success");
    		result.put("data", r);
    	}
    	else {
    		result.put("code", "fail");
    		if ("SUCCESS".equals(r.get("return_code")) && "FAIL".equals(r.get("result_code"))){
    			result.put("errorMsg", r.get("err_code_des"));
    		}
    		else if("FAIL".equals(r.get("return_code"))){
    			result.put("errorMsg", r.get("return_msg"));
    		}
    		else{
    			result.put("errorMsg", "支付下单失败!");
    		}
    	}
        
        return result;
	}

	/**
     * 关闭订单
     * out_trade_no 商户订单号
     */
	@RequestMapping(value = "/orderclose")
	@ResponseBody
	public void doOrderClose(HttpServletRequest request, HttpServletResponse response)  throws Exception {  
        
        Map<String, String> requestMap = DMUtil.parseRequestParams(request, false);
        
        HashMap<String, String> data = new HashMap<String, String>();
        data.put("out_trade_no", requestMap.get("out_trade_no"));
        
        WXPay wxpay = getWXPay(requestMap);

        try {
        	Map<String, String> r = wxpay.closeOrder(data);
        	response.setContentType("text/plain;charset=UTF-8");
        	
        	if( "SUCCESS".equals(r.get("return_code")) && "SUCCESS".equals(r.get("result_code")) ) {
        		response.getWriter().println("{\"code\": \"success\", \"data\": \"ok\"}");
        	}
        	else {
        		if ("SUCCESS".equals(r.get("return_code")) && "FAIL".equals(r.get("result_code"))){
        			response.getWriter().println("{\"code\": \"fail\", \"errorMsg\": \"" + r.get("err_code_des") + "\"}");
        		}
        		else{
        			response.getWriter().println("{\"code\": \"fail\", \"errorMsg\": \"订单关闭失败\"}");
        		}
        	}
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	/**
     * 查询订单
     * out_trade_no 商户订单号//, method = RequestMethod.POST 
     * appid
     * mchid
     */
	@RequestMapping(value = "/orderquery")
	@ResponseBody
	public void doOrderQuery(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Map<String, String> requestMap = DMUtil.parseRequestParams(request, false);
        
        HashMap<String, String> data = new HashMap<String, String>();
        data.put("out_trade_no", requestMap.get("out_trade_no"));
        
        WXPay wxpay = getWXPay(requestMap);
        try {
        	Map<String, String> r = wxpay.orderQuery(data);
        	response.setContentType("text/plain;charset=UTF-8");
        	
        	if( "SUCCESS".equals(r.get("return_code")) && "SUCCESS".equals(r.get("result_code")) ) {
        		if ("SUCCESS".equals(r.get("trade_state"))){
        			response.getWriter().println("{\"code\": \"success\", \"data\": \"支付成功\"}");
        		}
        		else{
        			response.getWriter().println("{\"code\": \"fail\", \"errorMsg\": \"" + r.get("trade_state_desc") + "\"}");
        		}
        	}
        	else {
        		response.getWriter().println("{\"code\": \"fail\", \"errorMsg\": \"订单查询失败\"}");
        	}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	/**
     * 发送模板消息
     * appid 小程序ID
     * touser 接收者（用户）的 openid
     * template_id 所需下发的模板消息的id
     * form_id submit 事件带上的 formId；支付场景下，为本次支付的 prepay_id
     * page 点击模板卡片后的跳转页面 pages/detail/detail?code=31807020012
     * data 模板内容
     * http://127.0.0.1:9000/tss/wx/api/sendmsg?appid=wx32ecfcea6f822096&touser=oNi3W5XOKp6GoFtHoX3iKa5gxyRg&template_id=X-4GDTiI44LCx8FpoFiVFzQQZPPXCVDJMtc2FSm9KyY&form_id=3e785af0aa9566717933951128b29b6a&data={\"keyword1\": {\"value\": \"豪邦物流\"},\"keyword2\": {\"value\": \"31806300002\"},\"keyword3\": {\"value\": \"您的订单已提交成功\"}}
	 * @throws IOException 
     */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/sendmsg")
	@ResponseBody
	public void sendMessage(HttpServletRequest request, HttpServletResponse response) throws IOException{
		
		Logger logger = Logger.getLogger(this.getClass());
		
		Map<String, String> requestMap = DMUtil.parseRequestParams(request, false);
		response.setContentType("text/plain;charset=UTF-8");
		String ret = null;
		
		try {
    		ret = new WXUtil().getAccessToken( requestMap.get("appid"));
    	} catch (Exception e) {
    		logger.error("WXUtil.getAccessToken failed: " + e.getMessage());
    		response.getWriter().println( "{\"code\": \"fail\", \"errorMsg\": \"获取accessToken接口失败\"}" );
    		return;
    	}
		
		Map<String, String> m = new ObjectMapper().readValue(ret, Map.class); 
		String accessToken = m.get("access_token");
		
		if(accessToken == null){
			response.getWriter().println( "{\"code\": \"fail\", \"errorMsg\": \"获取accessToken失败\"}" );
			return;
		}
		
		String url = "https://api.weixin.qq.com/cgi-bin/message/wxopen/template/send?access_token=" + accessToken;
		
		PostMethod postMethod = new PostMethod(url);
		postMethod.setRequestHeader("Content-Type", "application/json");
		
		Map<String, Object> da = new HashMap<String, Object>();
		da = new ObjectMapper().readValue( requestMap.get("data"), Map.class); 
		
		JSONObject data = new JSONObject();
		
		for (String in : da.keySet()) {
			Map<String, String> va = (Map<String, String>) da.get(in);
			JSONObject keyvalue = new JSONObject();
			for (String k : va.keySet()) {
				String str = va.get(k);
				keyvalue.put(k, str);
			}
			data.put(in, keyvalue);
		}
		
		JSONObject json = new JSONObject();
		json.put("touser", requestMap.get("touser"));
		json.put("template_id", requestMap.get("template_id"));
		json.put("form_id", requestMap.get("form_id"));
		json.put("page", requestMap.get("page"));
		json.put("data", data);
		
		String requestData = json.toString();
		
		HttpClient httpClient = new HttpClient();

		byte[] b = requestData.getBytes("UTF-8");
        InputStream is = new ByteArrayInputStream(b, 0, b.length);
        RequestEntity re = new InputStreamRequestEntity(is, b.length, "application/json; charset=UTF-8");
		postMethod.setRequestEntity(re);
		
		int statusCode = httpClient.executeMethod(postMethod);
		if (statusCode != 200) {
			response.getWriter().println( "{\"code\": \"fail\", \"errorMsg\": \"发送post请求失败\"}" );
		}
		
		String responseBody = postMethod.getResponseBodyAsString();
		
		Map<String, String> result = new ObjectMapper().readValue(responseBody, Map.class);
		
		if(result.get("errmsg").equals("ok")){
			response.getWriter().println( "{\"code\": \"success\", \"data\": \"OK\"}" );
		}
		else{
			response.getWriter().println( "{\"code\": \"fail\", \"errorMsg\": \"" + result.get("errmsg") + "\"}" );
		}
	}
	
	/**
	 * 公众号发送模板消息
	 * 
	 * 注：向www.boudata.com环境转本信息，需要在BD的白名单里，且请求要求位GET（POST乱码了）
	 */
	@RequestMapping(value = "/sendgzhmsg")
	@ResponseBody
	public void sendGZHMessage(HttpServletRequest request, HttpServletResponse response) throws IOException{
		Map<String, String> requestMap = DMUtil.parseRequestParams(request, false);
//		log.info(requestMap);
		
		response.setContentType("text/plain;charset=UTF-8");
		response.getWriter().println( wxService.sendWxGZHMsg(requestMap) );
	}

}
