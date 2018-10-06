package com.boubei.tssx.ftl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Table;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.ddl._Database;
import com.boubei.tss.dm.ddl._Field;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.framework.persistence.IEntity;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.URLUtil;

/**
 * 生成各类模板
 */
@Controller
@RequestMapping("/ftl")
public class FTL {
	
	@Autowired RecordService recordService;
	
	/**
	 * http://localhost:9000/tss/ftl/entitydef/6
	 */
	@RequestMapping(value = "/entitydef/{recordId}", produces = "text/plain;charset=utf-8")
	@ResponseBody
	public String recordDef2Model(@PathVariable("recordId") Long recordId) {
		_Database _db = recordService.getDB(recordId);
        List<Map<Object, Object>> fields = _db.getFields();
        
        String java = FileHelper.readFile(URLUtil.getResourceFileUrl("tss/_entity.ftl").getPath());
        Map<String, Object> dataMap = new HashMap<String, Object>();
        dataMap.put("tableName", _db.table);
        
        // def fields ---> java fileds
        Map<String, String> m = new HashMap<String, String>();
        m.put(_Field.TYPE_DATE, "Date");
        m.put(_Field.TYPE_DATETIME, "Date");
        m.put(_Field.TYPE_INT, "Long");
        m.put(_Field.TYPE_NUMBER, "Double");
        
        List<String> list = new ArrayList<String>();
        for(Map<Object, Object> f : fields) {
        	String type = (String) f.get("type");
        	type = (String) EasyUtils.checkNull( m.get(type), "String");
        	
        	list.add( "    private " + type + " "  + f.get("code"));
        }
        dataMap.put("fields", EasyUtils.list2Str(list, ";\n"));
		
        return EasyUtils.fmParse(java, dataMap);
	}

	/**
	 * http://localhost:9000/tss/ftl/tabledef?className=com.boubei.scm.wms.entity.Warehouse
	 */
	@RequestMapping(value = "/tabledef", produces = "text/plain;charset=utf-8")
	@ResponseBody
	public String model2RecordDef(String className) {

		String defJson = "[";
		Class<?> clazz = BeanUtil.createClassByName(className);
		Field[] fields = clazz.getDeclaredFields();
		for(Field f : fields) {
			List<String> list = new ArrayList<String>();
			
			String name = f.getName();
			if("id".equals(name)) continue;
			
			Class<?> type = f.getType();
			if( IEntity.class.isAssignableFrom(type) ) {
				list.add("'code':'" + name + "_id'");
				list.add("'type':'int'");
				list.add("'cwidth':'0'"); 
				list.add("'jsonUrl':'/tss/xdata/json/" + type.getAnnotation(Table.class).name() + "'");
			} 
			else {
				list.add("'code':'" + name + "'");
				if(type.equals(Integer.class) || type.equals(Long.class)) {
					list.add("'type':'int'");
				}
				if(type.equals(Float.class) || type.equals(Double.class)) {
					list.add("'type':'number'");
				}
				if(type.equals(Date.class)) {
					list.add("'type':'datetime'");
				}
			}
			
			Column column = f.getAnnotation(Column.class); // 取得注释对象
			if( column != null ) {
				if( !column.nullable() ) {
					list.add("'nullable':'false'");
				}
				if( column.unique() ) {
					list.add("'unique':'true'");
				}
				if( column.length() >= 500) {
					list.add("'width':'300px'");
					list.add("'height':'80px'");
				}
			}
			TssColumn _column = f.getAnnotation(TssColumn.class);
			String label = name;
			if( _column != null ) {
				label = _column.label();
				if( _column.isParam() ) {
					list.add("'isParam':'true'");
				}
				if( !EasyUtils.isNullOrEmpty(_column.defaultVal()) ) {
					list.add("'defaultVal':'" +_column.defaultVal()+ "'");
				}
				if( !EasyUtils.isNullOrEmpty(_column.checkReg()) ) {
					list.add("'checkReg':'" +_column.checkReg()+ "'");
				}
				if( !EasyUtils.isNullOrEmpty(_column.errorMsg()) ) {
					list.add("'errorMsg':'" +_column.errorMsg()+ "'");
				}

				if( !EasyUtils.isNullOrEmpty(_column.calign()) ) {
					list.add("'calign':'" +_column.calign()+ "'");
				}
				if( !EasyUtils.isNullOrEmpty(_column.cwidth()) ) {
					list.add("'cwidth':'" +_column.cwidth()+ "'");
				}
				if( !EasyUtils.isNullOrEmpty(_column.width()) ) {
					list.add("'width':'" +_column.width()+ "'");
				}
				if( !EasyUtils.isNullOrEmpty(_column.height()) ) {
					list.add("'height':'" +_column.height()+ "'");
				}
				if( !EasyUtils.isNullOrEmpty(_column.options()) ) {
					list.add("'options':'" +_column.options()+ "'");
				}
				if( !EasyUtils.isNullOrEmpty(_column.jsonUrl()) ) {
					list.add("'jsonUrl':'" +_column.jsonUrl()+ "'");
				}
			}
			list.add(0, "'label':'" + label + "'");
				
			defJson += "\n  {" +EasyUtils.list2Str(list)+ "},";
		}
		
		return defJson.substring(0, defJson.length() - 1) + "\n]\n\n表名：" + clazz.getAnnotation(Table.class).name();
	}

	/**
	 * http://localhost:9000/tss/ftl/easyui/3
	 * 
	 * 静态方案：record define + 模板(FTL) ==> HTML 
	 * 动态方案：record define ==> 模板(easyui_recorder.html) + 用户信息 ==> 最终呈现给用户的界面
	 * 涉及数据表操作权限、字段权限
	 */
	@RequestMapping(value = "/easyui/{recordId}", produces = "text/html; charset=utf-8")
    @ResponseBody
    public String genEasyUIHtml(@PathVariable("recordId") Long recordId) {
        _Database _db = recordService.getDB(recordId);
        List<Map<Object, Object>> fields = _db.getFields();
        
        String html = FileHelper.readFile(URLUtil.getResourceFileUrl("tss/_easyui.ftl").getPath());
        Map<String, Object> dataMap = new HashMap<String, Object>();
        dataMap.put("recordId", _db.table);
        dataMap.put("recordName", _db.recordName);
        dataMap.put("recordFile", "false");
        
        String gridFields, recordForm = "", queryForm = "";
        
        // fields ---> gridFileds
        List<String> list = new ArrayList<String>();
        list.add("{field: 'ck', checkbox: true}");
        double baseWidth = Math.ceil(100/(fields.size()+1));
        
        for(Map<Object, Object> f : fields) {
        	Object width = f.containsKey("cwidth") ? f.get("cwidth") : baseWidth + "%";
        	Object align = f.containsKey("calign") ? f.get("calign") : "center";

        	list.add("{field: '" +f.get("code")+ "', title: '" +f.get("label")+ "', width: '" +width+ "', align: '" +align+ "'}");
        	if( "file".equals( f.get("type") ) ){
        		dataMap.put("recordFile", f.get("code"));
        	}
        	
        }
        list.add("{field: 'fileNum', title: '附件', width: '" + baseWidth + "%', align: 'center'}");
        gridFields = "[\n" +EasyUtils.list2Str(list, ",\n  ")+ "\n]";
        dataMap.put("gridFileds", gridFields);
        
        // fields ---> recordFrom
        int cospan = Math.min( fields.size()/6+1 , 3 );
        int index = 0;
        String td = "<td class='label'>$label</td>\n"
        		   +"<td>\n"
        			   +"<input name='$code' id='$code' class='$class' $editable $recision $required/>\n"
                   +"</td>\n";
        for(Map<Object, Object> f : fields) {
        	if( index%cospan == 0 ){
        		recordForm += "<tr>\n";
        	}        	
        	recordForm += transType(f,td);
			if( index%cospan == cospan - 1 ){
        		recordForm += "</tr>\n";
        	}	
        	index ++;
        }
        
        if(!recordForm.endsWith("</tr>\n")){
        	recordForm += "</tr>\n";
        }
        
        recordForm = "<table class='l'>\n" + recordForm + "</table>";
        dataMap.put("recordForm", recordForm);
        
        // fields ---> queryForm
        List<String> paramFields = new ArrayList<String>();
        for(Map<Object, Object> f : fields) {
        	String isparam = (String) f.get("isparam");
			if( "true".equals(isparam) ) {
				paramFields.add( "query_" + (String) f.get("code") );
				String tr = "<tr><td class='label'>$label</td>\n"
			        	+"<td>\n"
			                +"<input name='query_$code' id='query_$code' class='$class' $editable/>\n"
			            +"</td></tr>\n";
				queryForm += transType(f,tr);
			}		
        }
        queryForm = "<table class='l'>\n" + queryForm + "</table>";
        dataMap.put("queryForm", queryForm);
        dataMap.put("paramFields", DMUtil.insertSingleQuotes(EasyUtils.list2Str(paramFields)));
        
        html = EasyUtils.fmParse(html, dataMap );
        
        return html;
    }
	

	public static int getIndex(String types[],String type){
		int index = -1;
		for(int i = 0;i < types.length;i++){
			if( types[i].equals(type) ){
				index = i;
			}
		}
		return index;
	}
	
	public static String transType(Map<Object,Object> f,String td){
		String types[] = {"string","int","number","date","datetime","file"};
    	String newType[] = {"easyui-textbox","easyui-numberbox","easyui-numberbox","easyui-datebox","easyui-datetimebox","easyui-combobox"};
    	String type = "";
    	String editable = "";
    	String recision = "";
    	if( f.get("type")==null ){
    		type = "easyui-textbox";
    	}else{
    		type = newType[getIndex(types,f.get("type").toString())];
    	}
    	
    	if( "easyui-datebox".equals(type) || "easyui-datetimebox".equals(type) ){
    		editable = "editable='false'";
    	}
    	
    	if( "number".equals( f.get("type") ) ){
    		recision="recision='2'";
    	}
    	
    	String required = "";
    	if(f.get("nullable")==null){
    		
    	}else if( "false".equals(f.get("nullable").toString()) ){
    		required = "required";
    	}
    	
    	return td.replace("$label", (String) f.get("label"))
				 .replace("$code", (String) f.get("code"))
				 .replace("$class", type)
			     .replace("$required", required)
				 .replace("$recision", recision)
				 .replace("$editable", editable);	 
	}
}
