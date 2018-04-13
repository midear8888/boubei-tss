package com.boubei.tss.dm.record.workflow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WFStep {
	
	public String id;
	public String status;
	public Map<String, List<WFButton>> role_btn = new HashMap<String, List<WFButton>>();
	public Map<String, List<WFButton>> user_btn = new HashMap<String, List<WFButton>>();
}