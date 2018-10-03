/* ==================================================================   
 * Created [2016-3-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.boubei.tss.EX;
import com.boubei.tss.PX;
import com.boubei.tss.cache.Cacheable;
import com.boubei.tss.cache.Pool;
import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.dm.ddl._Field;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.record.permission.RecordPermission;
import com.boubei.tss.dm.report.ScriptParser;
import com.boubei.tss.dm.report.ScriptParserFactory;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.framework.web.display.xform.XFormEncoder;
import com.boubei.tss.modules.param.Param;
import com.boubei.tss.modules.param.ParamManager;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.permission.PermissionHelper;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.URLUtil;

public class DMUtil {
	
	static Logger log = Logger.getLogger(DMUtil.class);
	
	/**
	 * 按角色控制数据源下拉列表，角色信息配置在数据源参数项的备注里，多个角色逗号分隔。
	 */
	public static void setDSList(XFormEncoder xformEncoder) {
		try {
        	List<Param> dsItems = ParamManager.getComboParam(PX.DATASOURCE_LIST);
        	List<Param> _dsItems = new ArrayList<Param>();
        	for(Param ds : dsItems) {
        		Long creatorId = (Long) EasyUtils.checkNull(ds.getCreatorId(), UMConstants.ADMIN_USER_ID );
        		
        		List<Long> permiters = new ArrayList<Long>();
        		permiters.add( UMConstants.ADMIN_USER_ID ); 
				permiters.add( Environment.getUserId() );
        		
        		// 管理员创建的数据源都可见，其它的只有创建人（通常为开发者）自己可见
				if( permiters.contains( creatorId ) || Environment.isAdmin() ) {
					_dsItems.add(ds);
        		} 
        	}
        	
            xformEncoder.fixCombo("datasource", _dsItems);	
        } catch (Exception e) {
        }
	}
	
	/*
	 *  注：通过tssJS.ajax能自动过滤params里的空值，jQuery发送的ajax请求则不能
	 *  $.getJSON("/tss/xdata/json/dm_etl_task?name=任务&xx=abc", {"jobname": "定时器"}, function(data) { console.log(data); }, "POST");
	 *  
	 *  注：自定义的接口入口处，需要单独调用 URLUtil.parseQueryString 来替代 Request.getParamter()
	 */
    public static Map<String, String> getRequestMap(HttpServletRequest request, boolean isGet) {
    	// Tomcat 或 Jetty 默认已经对queryString上的参数 URLDecode 处理
    	Map<String, String[]> parameterMap = request.getParameterMap();
    	boolean apiCall = parameterMap.containsKey("uName");
    	
    	Map<String, String> requestMap = new LinkedHashMap<String, String>();
    	
    	// 处理通过queryString传递过来的中文参数，这些参数已经过URLEncode处理
    	String queryString = request.getQueryString();
		requestMap.putAll( URLUtil.parseQueryString(queryString) );
    	
    	// 没有经过URLEncode处理的GetRequest的参数，Tomcat对这一类参数默认为ISO-8859-1编码
    	for(String key : parameterMap.keySet()) {
			if(requestMap.containsKey(key)) continue;
			
			String[] values = parameterMap.get(key);
			String value = null;
			if( isGet || apiCall ) { // (tomcat7 or httpClient call API(uToken) )
				try {
					value = new String(values[0].getBytes("ISO-8859-1"), "UTF-8"); 
				} catch (UnsupportedEncodingException e) {
				}
			}
			requestMap.put( key, (String) EasyUtils.checkNull(value, values[0]) );
    	}
    	
    	requestMap.remove("_time");          // 剔除jsonp为防止url被浏览器缓存而加的时间戳参数
    	requestMap.remove("jsonpCallback"); // jsonp__x,其名也是唯一的
    	requestMap.remove("appCode");      // 其它系统向当前系统转发请求
    	requestMap.remove("ac");
    	
    	return requestMap;
    }
	
	public static String getExportPath() {
		return getConfigedPath(PX.ATTACH_PATH).replaceAll("\n", "");
	}
	
	public static String getConfigedPath(String pathParam) {
		String defaultPath = FileHelper.ioTmpDir();
		try {
			String configedPath = ParamManager.getValue(pathParam);
			if( new File(configedPath).exists() ) {
				defaultPath = configedPath;
			}
		} catch(Exception e) {
		}
		
		return defaultPath;
	}
	
	// 判断是否为区间查询（从 。。。 到 。。。），value格式：[2017-06-22,2027-08-01]
	public static String[] preTreatScopeValue(String value) {
		if(value == null) return new String[] { };
		
  		value = value.trim();
  		if(value.startsWith("[") && value.endsWith("]") && value.indexOf(",") > 0) {
  			String[] vals = value.substring(1, value.length() - 1).split(",");
  			if(vals.length == 1) {
  				return new String[] { vals[0] };
  			} else if(vals.length >= 2) {
  				return new String[] { (String) EasyUtils.checkNull(vals[0], vals[1]), vals[1] };  // [,2027-08-01]
  			} else {
  				return new String[] { };
  			}
  		}
  		return new String[] { value };
	}
	
    /** 为逗号分隔的每一个值加上单引号 */
    public static String insertSingleQuotes(String param) {
        if (param == null) return null;
        
        // 支持列表in查询，分隔符支持中英文逗号、中英文分号、空格、顿号
        param = param.replaceAll("，", ",").replaceAll(" ", ",").replaceAll("、", ",");
        if ( param.contains(",") ) {
        	if( param.indexOf("'") < 0 ) {
        		return "\'" + param.replaceAll(",", "\',\'") + "\'";
        	}
        	return param;
        } 
        else {
            return "\'" + param + "\'";
        }
    }
    
    /** 导出数据到CSV文件中时，需要对字段值里包含的特殊符号进行处理，以便可以在Excel中正常打开 */
    public static String preTreatVal(Object value) {
    	if(value == null) return "";
		
		String valueS = value.toString().replaceAll(",", "，"); // 导出时字段含英文逗号会错列
		valueS = valueS.replaceAll("\r\n", " ").replaceAll("\n", " "); // 替换掉换行符
		valueS = valueS.replaceAll("\"", ""); // 替换掉英文双引号
//		valueS = valueS.replaceAll("'", "");  // 替换掉英文单引号，SQL中含有单引号
		valueS = valueS.replaceAll(";", "；"); // 替换掉英文分号，mac os下csv默认用分号分隔
		
		valueS = Pattern.compile("\t|\r|\n").matcher(valueS).replaceAll(" "); // 保险起见，再替换一次
		
		return valueS; 
    }
	
  	public static Object preTreatValue(String value, Object type) {
  		
  		if(value == null) return null;
  		
  		type = EasyUtils.checkNull(type, _Field.TYPE_STRING);
  		type = type.toString().toLowerCase();
  		value = value.trim();
  		
  		if(_Field.TYPE_NUMBER.equals(type) || _Field.TYPE_INT.equals(type)) {
  			if( EasyUtils.isNullOrEmpty(value) ) return null;
  			
  			try {
  				if(value.indexOf(".") >= 0) {
  					value = value.replace("$", "").replace("￥", "").replaceAll(",|，", ""); // 金额
  	  				return EasyUtils.obj2Double(value);
  	  			}
  	  			return EasyUtils.obj2Long(value);
  			} catch(Exception e) {
				return null; // 如果输入的是空字符串等，会有异常
			}
  		}
  		else if(_Field.TYPE_DATE.equals(type) || _Field.TYPE_DATETIME.equals(type)) {
  			if( EasyUtils.isNullOrEmpty(value) ) return null;
  			
  			Date dateObj = DateUtil.parse(value);
  			if(dateObj == null) {
  				throw new BusinessException( EX.parse(EX.DM_01, value) );
  			} 
  			return new Timestamp(dateObj.getTime());
  		}
  		else {
  			// 过滤掉emoj表情符号 TODO 有待验证
  			value = value.replaceAll("[\\ud83c\\udc00-\\ud83c\\udfff]|[\\ud83d\\udc00-\\ud83d\\udfff]|[\\u2600-\\u27ff]", "*");
  			return EasyUtils.obj2String(value); // "null"、"undefined"均为空字符串处理
  		}
  	} 
  	
	public static Map<String, Object> getFreemarkerDataMap() {
    	Map<String, Object> fmDataMap = new HashMap<String, Object>();
        
      	// 加入登陆用户的信息
    	fmDataMap.put( "_" + DMConstants.USER_ID, Environment.getUserId());
      	fmDataMap.put(DMConstants.USER_ID, EasyUtils.obj2String(Environment.getUserId()));
      	fmDataMap.put(DMConstants.USER_CODE, Environment.getUserCode());
		fmDataMap.put(DMConstants.FROM_USER_ID, Environment.getUserInfo(DMConstants.FROM_USER_ID));
		
		for( Long role : Environment.getOwnRoles() ) {
			fmDataMap.put("role_" + role, role);
		}
		for( String role : Environment.getOwnRoleNames() ) {
			fmDataMap.put( role, role );
		}
		
		// 加入域账号过滤数据表条件的标准片段
		fmDataMap.put(DMConstants.FILTER_BY_DOMAIN, DMConstants.DOMAIN_CONDITION);
		
		/* 往dataMap里放入Session里的用户权限、角色、组织等信息，作为宏代码解析。 */
    	try {
    		HttpSession session = Context.getRequestContext().getRequest().getSession();
    		Enumeration<String> keys = session.getAttributeNames();
    		while(keys.hasMoreElements()) {
    			String key = keys.nextElement();
    			fmDataMap.put(key, session.getAttribute(key).toString());
    		}
    	} catch(Exception e) { }
		
    	fmDataMap.put(SSOConstants.USER_DOMAIN, Environment.getDomain()); // 非空
    	
		return fmDataMap;
    }

	/** 用Freemarker引擎解析脚本 */
	public static String freemarkerParse(String script, Map<String, ?> dataMap) {
		String rtScript = EasyUtils.fmParse(script, dataMap);
		if(rtScript.startsWith("FM-parse-error")) {
			Map<String, Object> paramsMap = new HashMap<String, Object>();
	    	for(String key : dataMap.keySet()) {
	    		if(key.startsWith("param") || key.startsWith("report.")) {
	    			paramsMap.put(key, dataMap.get(key));
	    		}
	    	}
	    	log.info("\n------------ params-----------: " + paramsMap + "\n" );
		}

	    return rtScript;
	}
	
	private static String _customizeParse(String script, Map<String, Object> dataMap) {
		Map<String, Object> data = new HashMap<String, Object>();
		data.putAll(getFreemarkerDataMap());
		data.putAll(dataMap);
 		
		ScriptParser scriptParser = ScriptParserFactory.getParser();
      	if(scriptParser == null) {
      		script = DMUtil.freemarkerParse(script, data);
      	} else {
      		script = scriptParser.parse(script, data);
      	}
      	
      	return script;
	}
	
	public static String fmParse(String script) {
		return _customizeParse(script, new HashMap<String, Object>());
	}
	
	public static String fmParse(String script, Map<String, ?> valuesMap) {
		return fmParse(script, valuesMap, false);
	}
	
	// 带录入表${rctable}解析，性能有一定影响
	public static String fmParse(String script, Map<String, ?> valuesMap, boolean withRCTable) {
		Map<String, Object> data = new HashMap<String, Object>();
		data.putAll( valuesMap );
		
		/*  自动为针对 数据表 的查询加上 按域过滤。 ${tableName} ==> 带按域过滤的子查询 */
		if(withRCTable) {
			Pool cache = CacheHelper.getNoDeadCache();
			String key = DMConstants.RECORD_TABLE_LIST;
			Cacheable o = cache.getObject(key );
	      	if( o == null ) {
	      		ICommonService commonService = Global.getCommonService();
	      		String hql = "select id, table from Record where type = 1 and disabled <> 1 ";
	      		o = cache.putObject(key, commonService.getList(hql)); 
	      	}
	      	
	      	@SuppressWarnings("unchecked")
	      	List<Object[]> tables = (List<Object[]>) o.getValue();
	      	
	      	PermissionHelper ph = PermissionHelper.getInstance();
	      	String permissionTable = RecordPermission.class.getName();
			List<Long> permissions = ph.getResourceIdsByOperation(permissionTable, Record.OPERATION_EDATA);
	      	permissions.addAll(ph.getResourceIdsByOperation(permissionTable, Record.OPERATION_VDATA));
	      	
	      	for(Object[] table : tables) {
	      		Long recordId = (Long) table[0];
	      		String tableName = (String) table[1];
	      		
	      		try {
					boolean visible = permissions.contains(recordId); // 浏览|编辑
					String filterS = wrapTable(tableName, visible, Environment.isAdmin());  // 默认按录入wrap
					
					data.put(tableName.toUpperCase(), filterS);
					data.put(tableName.toLowerCase(), filterS);
	      		} 
	      		catch(Exception e) { }
	      	}
		}
		
		script = _customizeParse(script, data);
		
		if( script.indexOf("${") >=0 ) {
			script = DMUtil.fmParse(script, data, withRCTable); // 再解析一次
      	}
      	
		return script;
	}
	
	public static String wrapTable(String tableName, boolean visible, boolean isAdmin) {
		if(isAdmin) return tableName;
		
		String innerSelect = "(select * from " +tableName+ " where 1=1 ";
		if(visible) {
			innerSelect += DMConstants.DOMAIN_CONDITION;
		} else {
			innerSelect += DMConstants.CREATOR_CONDITION;
		}
		return innerSelect + ") x";
	}
	
	/**
	 * 对paramValue进行检测，防止SQL注入
	 */
	public static String checkSQLInject(String paramValue) {
		String _pVal = (paramValue+"").toLowerCase();
		
		String sqlKeyword = "exec|alter|drop|create|insert|select|delete|update|from|like|master|truncate|declare"; // and|or|chr|mid|char|
		String[] sqlKeywords = sqlKeyword.split("\\|");
		for (String keyword : sqlKeywords) {
			if (_pVal.indexOf(keyword + " ") >=0 || _pVal.indexOf(" " + keyword) >=0 ) {
				return "inject-word:" + keyword; 
			}
		}
		
		if (_pVal.indexOf(" or ") >=0 || _pVal.indexOf(" and ") >=0) {
			return "inject-word:or|and"; 
		}
		
		// ' * % ; = - + > < ( )
		return paramValue.replaceAll("=|;|>|<", " "); 
		/* - + %号可能做为连接符存在字段值里：2016-10-12, today-1, today+1, %m-%d 
		 * |\\(|\\) : 东风重卡(17.5), 此类查询条件可能含有括号，先放开
		 * ' '合计' as 合计
		 * */
	}
	
	// 当参数值大于500个字符时，截断参数
	public static String cutParams(String params) {
		if (params != null && params.length() > 500) {
            params = params.substring(0, 500);
        }
		return params;
	}
	
    /*
     * 1、展示图标等扩展属性：一个图片地址 或 class，存放在remark里，如icon：
     *  icon:=<div class='icon icon-key tssicon'></div>
     *  icon:=<img src='/tss/images/icon_refresh.gif'/>
     *  
     * 2、允许在录入表备注里配置导入模板的列
     *  import_tl_fields:=车牌号,经度,纬度,速度
     *  import_tl_ignores:=gps时刻,航向速度
     * 
     * 3、LOGIC_DEL:=true 
     */
	public static String getExtendAttr(String remark, String attr) {
		String[] infos = EasyUtils.split(remark + "", "\n");
		for(String info : infos) {
			String[] arr = info.split(":=");
			if(arr.length == 2 && attr.equals(arr[0].trim()) ) {
				return arr[1].trim();
			}
		}
		
		return null;
	}
}
