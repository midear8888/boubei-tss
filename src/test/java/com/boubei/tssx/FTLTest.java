package com.boubei.tssx;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.AbstractTest4;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tssx.ftl.FTL;

public class FTLTest extends AbstractTest4 {
	
	@Autowired RecordService recordService;
	@Autowired FTL ftl;

	@Test
	public void test() {
//		System.out.println( ftl.model2RecordDef(Warehouse.class.getName()) );
		
		String tblDefine = "[ " +
				"	{'label':'申请人', 'code':'applier', 'type':'string'}," +
        		"	{'label':'日期', 'code':'fromDay', 'type':'date'}," +
        		"	{'label':'天数', 'code':'days', 'type':'number'}," +
        		"	{'label':'状态', 'code':'state'}," +
        		"   {'label':'实请天数','code':'wf_realdays','type':'number'}" +
        		"]";
		
		Record record = new Record();
		record.setName("请假提报");
		record.setType(1);
		record.setParentId(0L);
		
		record.setDatasource(DMConstants.LOCAL_CONN_POOL);
		record.setTable("tbl_auto");
		record.setDefine(tblDefine);
		
		recordService.createRecord(record);
		Long recordId = record.getId();
		
		System.out.println( ftl.recordDef2Model(recordId) );
		
		System.out.println( ftl.genEasyUIHtml(recordId) );
	}
	
}
