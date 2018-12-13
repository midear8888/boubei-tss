package com.boubei.tss.dm.record.file;

import java.util.List;

import com.boubei.tss.dm.ddl._Database;

/**
 * 空数据校验器，for简化版本导入自定义类（继承ImportCSV）
 */
public class EmptyDataVaild implements IDataVaild {


	public void vaild(_Database _db, List<List<String>> rows, List<String> headers, String uniqueCodes, List<String> valSQLFields,
			List<String> errorLines, List<Integer> errLineIndexs) {
		
	}
	
}
