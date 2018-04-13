package com.boubei.tss.dm.record.workflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Element;

import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.ddl._Database;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.XMLDocUtil;

public class WFDefine {
	
	public WFDefine(String wf, _Database _db) {
		this.content = wf;
		
		HashMap<String, Object> dataMap = new HashMap<String, Object>();
		dataMap.put("all", "");
		dataMap.put("creator", "");
		for( String filed : _db.fieldCodes ) {
			dataMap.put(filed, "");
		}
		wf = DMUtil.freemarkerParse( wf, dataMap );
		this.xml = XMLDocUtil.dataXml2Doc(wf);
	}
	
	public String content;
	public Document xml;
	
	public Map<String, WFStep> steps = new HashMap<String, WFStep>();
	public Map<String, WFButton> btns = new HashMap<String, WFButton>();
	
	public String currStep;
	
	public static WFDefine parse(String workflow, _Database _db) {
		if(EasyUtils.isNullOrEmpty(workflow)) return null;
		
		WFDefine wf = new WFDefine(workflow, _db);
		
		List<Element> nodes = XMLDocUtil.selectNodes(wf.xml, "/process/buttonSet/button");
		for( Element node : nodes ) {
			WFButton btn = new WFButton();
			btn.id = node.attributeValue("id");
			btn.name = node.attributeValue("name");
			btn.script = XMLDocUtil.getNodeText(node);
			
			wf.btns.put(btn.id, btn);
		}
		
		nodes = XMLDocUtil.selectNodes(wf.xml, "/process/steps/step");
		for( Element node : nodes ) {
			WFStep step = new WFStep();
			step.id = node.attributeValue("id");
			step.status = node.attributeValue("status");
			
			List<Element> roles = XMLDocUtil.selectNodes(node, "role");
			for(Element role : roles) {
				String[] btnIDs = role.getTextTrim().split(",");
				List<WFButton> list = new ArrayList<WFButton>();
				for(String btnID : btnIDs) {
					list.add( wf.btns.get(btnID) );
				}
				step.role_btn.put(role.attributeValue("value"), list);
			}
			
			List<Element> users = XMLDocUtil.selectNodes(node, "user");
			for(Element user : users) {
				String[] btnIDs = user.getTextTrim().split(",");
				List<WFButton> list = new ArrayList<WFButton>();
				for(String btnID : btnIDs) {
					list.add( wf.btns.get(btnID) );
				}
				step.role_btn.put(user.attributeValue("value"), list);
			}
			
			wf.steps.put(step.id, step);
		}
		
		return wf;
	}
}
