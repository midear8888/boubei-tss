package com.boubei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.boubei.tss.dm.DataExport;
import com.boubei.tss.dm.Excel;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.MathUtil;

public class JL  {
	
	public static void main(String[] args) {
		f1();
	}
	
	public static void f1() {
		String[] centers = "青岛,大连,宁波,深圳,天津,广州,上海".split(",");
		Date startDay = DateUtil.parse("2018-08-01");
		
		int x = 1;
		
		List<Object[]> data = new ArrayList<>();
		for(String c : centers) {
			x++;
			for(int i = 0; i < 31; i++) {
				Date d = DateUtil.addDays(startDay, i);
				
				Object[] o = new Object[4];
				o[0] = c ;
				o[1] = DateUtil.format(d);
				o[2] = MathUtil.randomInt(3) + x + 4;
				
				data.add(o);
			}
		}
		
		String fileName = DataExport.exportCSV( data , Arrays.asList("分拨,日期,出勤人数") );
		System.out.print(DataExport.getExportPath() + "/" + fileName);
		Excel.csv2Excel(DataExport.getExportPath() + "/" + fileName);
	}
	
	public static void f2() {
		String[] centers = "青岛,大连,宁波,深圳,天津,广州,上海".split(",");
		Date startDay = DateUtil.parse("2018-08-01");
		
		int x = 1;
		
		List<Object[]> data = new ArrayList<>();
		for(String c : centers) {
			x++;
			for(int i = 0; i < 31; i++) {
				Date d = DateUtil.addDays(startDay, i);
				
				for(int j = 0; j < 50; j++) {
					Object[] o = new Object[4];
					o[0] = ("JL" + x + DateUtil.format(d) + (j+1)).replaceAll("-", "") ;
					o[1] = MathUtil.randomInt(1000) + 80*x;
					o[2] = c;
					o[3] = DateUtil.format(d);
					
					data.add(o);
				}
				
			}
		}
		
		String fileName = DataExport.exportCSV( data , Arrays.asList("运单号,重量,分拨,操作日期") );
		System.out.print(DataExport.getExportPath() + "/" + fileName);
		Excel.csv2Excel(DataExport.getExportPath() + "/" + fileName);
	}


}
