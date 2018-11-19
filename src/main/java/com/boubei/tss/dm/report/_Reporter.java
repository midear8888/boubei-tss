/* ==================================================================   
 * Created [2016-3-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.report;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.EX;
import com.boubei.tss.PX;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.DataExport;
import com.boubei.tss.dm.Excel;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.report.log.AccessLogRecorder;
import com.boubei.tss.framework.Config;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.persistence.pagequery.PageInfo;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.web.display.grid.DefaultGridNode;
import com.boubei.tss.framework.web.display.grid.GridDataEncoder;
import com.boubei.tss.framework.web.display.grid.IGridNode;
import com.boubei.tss.framework.web.filter.Filter8APITokenCheck;
import com.boubei.tss.framework.web.mvc.BaseActionSupport;
import com.boubei.tss.modules.log.IBusinessLogger;
import com.boubei.tss.modules.log.Log;
import com.boubei.tss.modules.param.ParamManager;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MailUtil;
import com.boubei.tss.util.URLUtil;

@Controller
@RequestMapping( {"/data", "/data/api"} )
public class _Reporter extends BaseActionSupport {
    
    @Autowired ReportService reportService;
    
	@RequestMapping("/{idCodeName}/define")
    @ResponseBody
    public Object getReportDefine(HttpServletRequest request, @PathVariable("idCodeName") String idCodeName) {
		// 如果是【报表名：rpName】参数传过来，优先通过报表名查询
		String queryString = request.getQueryString();
		String rpName =  URLUtil.parseQueryString(queryString).get("rpName");
		
		rpName = (String) EasyUtils.checkNull(rpName, request.getParameter("rpName"));
		idCodeName = (String) EasyUtils.checkNull(rpName, idCodeName);
		
		Long reportId = reportService.getReportId(idCodeName);    	
		Report report = reportService.getReport(reportId);
		
		String name = report.getName();
		String param = report.getParam();
		String displayUri = report.getDisplayUri();
		boolean hasScript = !EasyUtils.isNullOrEmpty(report.getScript());
		Integer mailable  = report.getMailable();
		String remark = EasyUtils.obj2String( report.getRemark() );
		String queryUri = report.getParamUri();
		
		return new Object[] {name, param, displayUri, hasScript, mailable, remark, queryUri, reportId};
    }
	
    /**
     * 1、完成接口调用时令牌校验 & 自动登录
     * 2、根据每个报表的具体配置来确定使用具体的缓存策略。可分为：不缓存、按用户缓存、按参数缓存、按域缓存。
     * 注：加入企业域后，SQL里带上了${filterByDomain}，需要再加一种，按域缓存
     */
    private Object checkLoginAndCache(HttpServletRequest request, Long reportId) {
    	
    	Report report = reportService.getReport(reportId, false);
    	String script = (report.getScript()+"").toLowerCase();
    	
    	/* 其它系统调用接口时，传入其在TSS注册的用户ID; 检查令牌，令牌有效则自动完成登陆 */
    	Filter8APITokenCheck.checkAPIToken(request, report);
    	
    	/* 如果传入的参数要求不取缓存的数据，则返回当前时间戳作为userID，以触发缓存更新。*/
    	boolean reportCache = Config.TRUE.equalsIgnoreCase(ParamManager.getValue(PX.REPORT_CACHE, Config.TRUE));
    	Object cacheFlag;
    	if( !reportCache || Config.TRUE.equals(request.getParameter("noCache")) ) {
    		cacheFlag = System.currentTimeMillis(); // 按时间戳缓存，白存了，永远无法再次命中
    	}
    	else if( Config.TRUE.equals(request.getParameter("uCache")) 
    			|| Pattern.compile("from[\\s]*\\$\\{").matcher(script).find() ) { // 面向数据表查询
    		cacheFlag = Environment.getUserId();  // 按【用户 + 参数】缓存
    	}
    	else {
    		cacheFlag = Environment.getDomain(); // 按查询【域 + 参数】缓存
    	}
    	return cacheFlag;
    }
 
    @RequestMapping("/{reportId}/{page}/{pagesize}")
    public void showAsGrid(HttpServletRequest request, HttpServletResponse response, 
            @PathVariable("reportId") Long reportId, 
            @PathVariable("page") int page,
            @PathVariable("pagesize") int pagesize) {
    	
    	long start = System.currentTimeMillis();
    	Map<String, String> requestMap = DMUtil.parseRequestParams(request, false);
		Object cacheFlag = checkLoginAndCache(request, reportId);
		SQLExcutor excutor = reportService.queryReport(reportId, requestMap, page, pagesize, cacheFlag);
    	
		AccessLogRecorder.outputAccessLog(reportService, reportId, "showAsGrid", requestMap, start);
        
        List<IGridNode> list = new ArrayList<IGridNode>();
        for(Map<String, Object> item : excutor.result) {
            DefaultGridNode gridNode = new DefaultGridNode();
            gridNode.getAttrs().putAll(item);
            list.add(gridNode);
        }
        GridDataEncoder gEncoder = new GridDataEncoder(list, excutor.getGridTemplate());
        
        PageInfo pageInfo = new PageInfo();
        pageInfo.setPageSize(pagesize);
        pageInfo.setTotalRows(excutor.count);
        pageInfo.setPageNum(page);
        
        print(new String[] {"ReportData", "PageInfo"}, new Object[] {gEncoder, pageInfo});
    }
    
    /**
     * 可直接导出发送电子邮件：http://localhost:9000/tss/data/export/42/1/10000?paramX=xxx&email=boubei@163.com
     * 支持用report name
     */
    @RequestMapping("/export/{report}/{page}/{pagesize}")
    public void exportAsCSV(HttpServletRequest request, HttpServletResponse response, 
            @PathVariable("report") Object report, 
            @PathVariable("page") int page,
            @PathVariable("pagesize") int pagesize) {
        
    	Map<String, String> requestMap = DMUtil.parseRequestParams(request, true);
    	Long reportId = reportService.getReportId( report.toString() );
    	
		Object cacheFlag = checkLoginAndCache(request, reportId);
		String email = requestMap.remove("email");
		
		long start = System.currentTimeMillis();
		SQLExcutor excutor = reportService.queryReport(reportId, requestMap, page, pagesize, cacheFlag);
		
		String fileName = report + "-" + Environment.getUserId() + ".csv";
        String exportPath;
        
        // 如果导出数据超过了pageSize（前台为导出设置的pageSize为10万），则不予导出并给与提示
		if(pagesize > 0 && excutor.count > pagesize) {
			List<Object[]> result = new ArrayList<Object[]>();
			result.add(new Object[] {"您当前查询导出的数据有" +excutor.count+ "行, 超过了系统单次导出上限【" +pagesize+ "行】，请缩短查询范围，分批导出。"});
			
			exportPath = DataExport.getExportPath() + "/" + fileName;
			DataExport.exportCSV(exportPath, result, Arrays.asList("result"));
		}
		else {
			// 先输出查询结果到服务端的导出文件中
			exportPath = DataExport.exportCSV(fileName, excutor.result, excutor.selectFields);
		}
		
		if( email != null ) {
			String subject = EX.TIMER_REPORT + reportService.getReport(reportId, false).getName();
			String html = "详细请查收附件";
			
			String _ms = (String) EasyUtils.checkNull( requestMap.get("_ms"), MailUtil.DEFAULT_MS );
			MailUtil.sendHTML(subject, html, email.split(","), _ms, new File(exportPath));
		}
		else { // 下载上一步生成的附件
	        DataExport.downloadFileByHttp(response, exportPath, excutor.result.size() >= 655350);
		}
        
        AccessLogRecorder.outputAccessLog(reportService, reportId, "exportAsCSV", requestMap, start);
    }
    
    /**
     * 将前台（一般为生成好的table数据）数据导出成CSV格式
     */
    @RequestMapping("/export/data2csv")
    @ResponseBody
    public String[] data2CSV(HttpServletRequest request, HttpServletResponse response) {
    	Map<String, String> requestMap = DMUtil.parseRequestParams(request, false);
    	String name = requestMap.get("name");
    	String data = requestMap.get("data");
		
		String fileName = name + "-" + System.currentTimeMillis() + ".csv";
        String exportPath = DataExport.getExportPath() + "/" + fileName;
 
		// 先输出内容到服务端的导出文件中
        DataExport.exportCSV(exportPath, data);
        exportPath = Excel.csv2Excel(exportPath);
        
        // 记录导出日志
 		Log excuteLog = new Log(name, Environment.getUserCode() + "导出了网页数据：" + fileName );
     	excuteLog.setOperateTable("网页数据导出");
         ((IBusinessLogger) Global.getBean("BusinessLogger")).output(excuteLog);
        
        return new String[] { fileName };
    }
    
    @RequestMapping("/download")
    public void download(HttpServletRequest request, HttpServletResponse response) {
    	String fileName = DMUtil.parseRequestParams(request, true).get("filename");
        String exportPath = DataExport.getExportPath() + "/" + fileName ;
        DataExport.downloadFileByHttp(response, exportPath);
    }
    
    /**
     * report可能是report的ID 也 可能是 Name.
     * 注：一次最多能取10万行。
     */
    @RequestMapping("/json/{report}")
    @ResponseBody
    public Object showAsJson(HttpServletRequest request, HttpServletResponse response, 
    		@PathVariable("report") String report) {
    	
    	/* 允许跨域访问。 经测试JQuery.ajax请求可以跨域调用成功，tssJS.ajax不行 */
    	response.addHeader("Access-Control-Allow-Origin", "*"); 
    	
    	Long reportId = reportService.getReportId(report);
    	
    	String jsonpCallback = request.getParameter("jsonpCallback"); // jsonp是用GET请求
    	Map<String, String> requestMap = DMUtil.parseRequestParams(request, jsonpCallback != null);
    	
    	Object page = requestMap.get("page");
    	Object pagesize = requestMap.get("pagesize");
    	pagesize = EasyUtils.checkNull(pagesize, requestMap.get("rows"), 10*10000); // easyUI用rows
    	
    	int _pagesize = EasyUtils.obj2Int(pagesize);
    	int _page = page != null ? EasyUtils.obj2Int(page) : 1;
    			
    	long start = System.currentTimeMillis();
        Object cacheFlag = checkLoginAndCache(request, reportId);
        requestMap.remove("uName");
        requestMap.remove("uToken");
        requestMap.remove("uSign");
		SQLExcutor excutor = reportService.queryReport(reportId, requestMap, _page, _pagesize, cacheFlag);
        
        AccessLogRecorder.outputAccessLog(reportService, reportId, "showAsJson", requestMap, start);
        
        if(page != null || requestMap.containsKey("rows")) {
        	Map<String, Object> returlVal = new HashMap<String, Object>();
        	returlVal.put("total", excutor.count);
        	returlVal.put("rows", excutor.result);
        	return returlVal;
        }
        
        return excutor.result;
    }
 
    @RequestMapping("/jsonp/{report}")
    public void showAsJsonp(HttpServletRequest request, HttpServletResponse response, 
    		@PathVariable("report") String report) {
    	
        // 如果定义了jsonpCallback参数，则为jsonp调用。示例参考：boubei-ui/JSONP.html
        String jsonpCallback = request.getParameter("jsonpCallback");
        jsonpCallback = (String) EasyUtils.checkNull(jsonpCallback, "console.log");
        
		String json = EasyUtils.obj2Json( showAsJson(request, response, report) );
    	print(jsonpCallback + "(" + json + ")");
    }
}
