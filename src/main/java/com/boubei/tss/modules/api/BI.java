package com.boubei.tss.modules.api;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.DataExport;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.dml.SqlConfig;
import com.boubei.tss.dm.dml.SqlConfig.Script;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.web.mvc.BaseActionSupport;
import com.boubei.tss.util.EasyUtils;

/**
 * 控制访问权限，在sqlConfig xml里
 * 
 * http://localhost:9000/tss/api/bi/sql/codeScan?code=A191101001X
 * 
 */
@Controller
@RequestMapping({ "/api/bi", "/api/data" })
public class BI extends BaseActionSupport {
	
	@Autowired APIService service;
	
	@RequestMapping("/jsonp/{sqlCode}")
    public void querySQL4JSONP(HttpServletRequest request, @PathVariable("sqlCode") String sqlCode) {
    	
        String jsonpCallback = request.getParameter("jsonpCallback");
        jsonpCallback = (String) EasyUtils.checkNull(jsonpCallback, "console.log");
        
		String json = EasyUtils.obj2Json( querySQL_(request, sqlCode) );
    	print(jsonpCallback + "(" + json + ")");
    }

	/**
	 * 分页 {"total": 99, "rows": [......]}
	 */
	@RequestMapping(value = "/{sqlCode}", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> querySQL(HttpServletRequest request, @PathVariable("sqlCode") String sqlCode) {
		SQLExcutor ex = _excuteSQL(request, sqlCode, 10000, "query");
		return ex.toEasyUIGrid();
	}

	/**
	 * 默认不分页
	 */
	@RequestMapping(value = "/sql/{sqlCode}", method = RequestMethod.GET)
	@ResponseBody
	public Object querySQL_(HttpServletRequest request, @PathVariable("sqlCode") String sqlCode) {
		Map<String, String> params = DMUtil.parseRequestParams(request, true);

		SQLExcutor ex = _excuteSQL(request, sqlCode, 10000, "query");
		if ( params.containsKey("page") || params.containsKey("rows") ) {
			return ex.toEasyUIGrid();
		}
		return ex.result;
	}

	private SQLExcutor _excuteSQL(HttpServletRequest request, String sqlCode, int maxPagesize, String tag) {
		long ts = System.currentTimeMillis();
		
		Map<String, String> params = DMUtil.parseRequestParams(request, true);
		Object cacheFlag = EasyUtils.checkTrue(params.containsKey("cached"), Environment.getUserId(), ts);
    	int pagesize = EasyUtils.obj2Int( EasyUtils.checkNull(params.get("pagesize"), params.get("rows"), maxPagesize) );
		
		SQLExcutor ex = service.queryByScript(sqlCode, params, Math.min(pagesize, maxPagesize), tag, cacheFlag);

		return ex;
	}

	@RequestMapping("/export/sql/{sqlCode}")
	public void exportSQL(HttpServletRequest request, HttpServletResponse response, @PathVariable("sqlCode") String sqlCode) {

		SQLExcutor ex = _excuteSQL(request, sqlCode, 10 * 10000, "export");
		List<Map<String, Object>> data = ex.result;
		List<String> fields = ex.selectFields;
		
		Script script = SqlConfig._getScript(sqlCode, request.getParameter("sqlPath"));
		String cfields = request.getParameter("cfields");
		if( !EasyUtils.isNullOrEmpty(cfields) ) {
			fields = Arrays.asList( cfields.split(",") );
			
			for( Map<String, Object> row : data ) {
				for( Iterator<String> it = row.keySet().iterator(); it.hasNext(); ) {
					String field = it.next();
					if( !fields.contains(field) ) {
						it.remove();
					}
				}
			}
		}

		// 先输出查询结果到服务端的导出文件中
		String fileName = script.name + "-" + Environment.getUserId() + ".csv";
		String charSet = (String) EasyUtils.checkNull(request.getParameter(DataExport.CHARSET), DataExport.SYS_CHAR_SET);
		String exportPath = DataExport.exportCSV(fileName, data, fields, charSet);

		// 下载上一步生成的附件
		DataExport.downloadFileByHttp(response, exportPath, charSet, data.size() >= 655350);
	}
	
	
	@RequestMapping(value = "/recordfield/{table}", method = RequestMethod.GET)
	@ResponseBody
	public List<?> queryRecordFields(@PathVariable("table") String table) {
		return BIDataProcess.getRecordFields(Environment.getDomain(), table, Environment.getOwnRoles());
	}

}
