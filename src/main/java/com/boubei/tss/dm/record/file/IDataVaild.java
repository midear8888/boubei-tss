package com.boubei.tss.dm.record.file;

import java.util.List;

import com.boubei.tss.dm.ddl._Database;

/**
 * 行级异常提醒、导入失败记录单独生成文件 + 一列【异常描述】
 * 返回提示：新增多少行、修改多少行、失败多少行【下载】
 */
public interface IDataVaild {
	
	void vaild(_Database _db, String[] rows, String[] headers, List<String> valSQLFields,
			List<String> errorLines, List<Integer> errLineIndexs, List<Integer> emptyLineIndexs);

}
