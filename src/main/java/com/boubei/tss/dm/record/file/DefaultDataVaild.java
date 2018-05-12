package com.boubei.tss.dm.record.file;

import java.util.ArrayList;
import java.util.List;

import com.boubei.tss.EX;
import com.boubei.tss.dm.ddl._Database;

/**
 * TSS默认数据校验器
 */
public class DefaultDataVaild implements IDataVaild {

	public List<Integer> vaild(_Database _db, String[] rows, String[] headers, List<String> errLines) {
		List<Integer> errLineIndexs = new ArrayList<Integer>();
		
		for(int index = 1; index < rows.length; index++) { // 第一行为表头，不要
			
			String row = rows[index];
			String[] fieldVals = (row+ " ").split(",");
			
			// 0、检查列数是否和表头列数相等
			if(fieldVals.length != headers.length) {
				String errLine = index + "," + EX.parse(EX.DM_23, index).replaceAll(",", "，") + "," + row;
				
				errLines.add(errLine);
				errLineIndexs.add(index);
				continue;
			}
			
			// 1、nullable、unique、type 校验
			
			// 2、正则表达式校验
			
			// 3、默认值校验
			
			// 4、根据默认值补齐字段信息：自动取号、关联字段值获取等
			
		}
		
		return errLineIndexs ;
	}
	
}
