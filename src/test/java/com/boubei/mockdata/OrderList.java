package com.boubei.mockdata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.boubei.tss.dm.DataExport;
import com.boubei.tss.dm.Excel;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.MathUtil;

/**
 * 单号、日期、货品、数量、仓库、省、销售平台
 */
public class OrderList  {
	
	public static void main(String[] args) {
		f1();
	}
	
	static String[] provinces = new String[]{"北京","天津","上海","重庆","河北","山西","辽宁","吉林","黑龙江","江苏","浙江","安徽","福建","江西","山东",
			"河南","湖北","湖南","广东","海南","四川","贵州","云南","陕西","甘肃","青海","内蒙古","广西","西藏","宁夏","新疆维吾尔自治区","香港","澳门","台湾"};
	static int[] provinces1 = new int[]{50,30,55,35,60,40,45,30,28,90,95,60,50,44,75,
		70,72,66,100,10,80,15,18,40,13,4,8,30,3,10,15,3,3,3};
	static int[] provinces2 = new int[]{3,3,0,4,3,3,3,3,3,0,0,0,1,1,3,
		2,4,4,1,1,4,4,4,2,2,2,2,2,2,2,2,1,1,1};
	
	static String[] warehouses = new String[]{"上海仓","广州仓","西安仓","北京仓","武汉仓"};
	static String[] pingtais = new String[]{"天猫","京东","一号店","网易严选"};
		
	public static void f1() {
		
		List<String> skus = new ArrayList<>();
		for(int x = 1; x <= 100; x++) {
			String sku = "H1800" + x;
			int factor = MathUtil.randomInt(100) + 1;
			for(int i = 1; i <= factor; i++) {
				skus.add(sku);
			}
		}
		
		Date startDay = DateUtil.parse("2017-01-01");
		
		List<Object[]> data = new ArrayList<>();
		List<String> orderNos = new ArrayList<>();
		
		int index = 0;
		for(String province : provinces) {
			String warehouse = warehouses[ provinces2[index] ];
			for(int i = 0; i < 366; i++) {
				Date d = DateUtil.addDays(startDay, i);
				
				int factor = MathUtil.randomInt( provinces1[index] / 3 );
				if( DateUtil.getDayOfWeek(d) == 6 ) {
					factor = factor*13/10;
				}
				if( d.after( DateUtil.parse("2018-11-11") ) && d.before( DateUtil.parse("2018-12-15") ) ) {
					factor = factor*2;
				}
				
				for(int j = 0; j < factor; j++) {
					String pingtai = pingtais[0];
					if( data.size() % 10 >= 5  && data.size() % 10 <= 7 ) {
						pingtai = pingtais[1];
					}
					if( data.size() % 10 == 8 ) {
						pingtai = pingtais[2];
					}
					if( data.size() % 10 == 9 ) {
						pingtai = pingtais[2 - MathUtil.randomInt(2)];
					}
					// 品项数量
					int px = Math.abs(MathUtil.randomInt(3) - MathUtil.randomInt(2));
					String orderNo = "SO" + (orderNos.size() + 1);
					for(int n = 0; n < px; n++) {
						String sku = skus.get( MathUtil.randomInt(skus.size()-1) );
						
						Object[] o = new Object[7];
						o[0] = orderNo;
						o[1] = DateUtil.format(d);
						o[2] = sku;
						o[3] = MathUtil.randomInt(3);
						o[4] = warehouse ;
						o[5] = province ;
						o[6] = pingtai;
						
						data.add(o);
						orderNos.add(orderNo);
						
						if(orderNos.size() % 10000 == 0) {
							System.out.println( Arrays.asList(o) );
						}
					}
				}
			}
			index++;
		}
		
		String fileName = DataExport.exportCSV( data , Arrays.asList("单号,日期,货品,数量,仓库,省,销售平台") );
		System.out.print(DataExport.getExportPath() + "/" + fileName);
		Excel.csv2Excel(DataExport.getExportPath() + "/" + fileName);
	}

}
