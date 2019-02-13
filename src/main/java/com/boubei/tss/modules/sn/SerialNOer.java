package com.boubei.tss.modules.sn;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.dm.ddl._Field;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;

/**
 *	取号器，支持指定前缀及一次获取多个连号
 *	配合数据表字段使用，在字段定义默认值填入：XX-yyMMddxxxx，则在界面新增或导入数据时自动调用本取号器
 	
 	$.getJSON("/tss/sn/O/1", {}, function(sns) { console.log(sns[0]); });
 	
 */
@Controller
@RequestMapping("/sn")
public class SerialNOer {
	
	private boolean isGlobal; // 全局流水号：账户流水号等
	
	public SerialNOer() { }
	
	public SerialNOer(boolean isGlobal) { 
		this.isGlobal = isGlobal;
	}

	@RequestMapping(value = "/{sntemplate}/{count}")
	@ResponseBody
	public synchronized List<String> create(@PathVariable("sntemplate") String sntemplate, @PathVariable("count") int count) {
		String domain = this.isGlobal ? UMConstants.DEFAULT_DOMAIN : Environment.getDomainOrign();
		
		return create(domain, sntemplate, count);
	}
	
	public synchronized List<String> create(String domain, String sntemplate, int count) {
		
		sntemplate = EasyUtils.obj2String(sntemplate).toLowerCase();
		if( !sntemplate.endsWith(_Field.SNO_xxxx) ) {  // eg: SO、ASN等
			sntemplate += _Field.SNO_yyMMddxxxx;
		}
		
		Date _day;
		String snMode;
		if(sntemplate.endsWith(_Field.SNO_yyMMddxxxx)) {
			_day = DateUtil.today();
			snMode = DateUtil.format(_day, "yyyyMMdd").substring(2);
		} else {
			_day = DateUtil.parse("2018-08-23");
			snMode = "";
		}
		String precode = sntemplate.replace(_Field.SNO_yyMMddxxxx, "").replace(_Field.SNO_xxxx, "").toUpperCase();
		
		// 如果域扩展表(x_domain)里明确维护了订单前缀
		precode = (isGlobal ? "" : EasyUtils.obj2String( Environment.getDomainInfo("prefix") ) ) + precode;
		
		ICommonService commonService = Global.getCommonService();
		domain = (String) EasyUtils.checkNull(domain, UMConstants.DEFAULT_DOMAIN);
		
		String hql = " from SerialNO where day = ? and precode = ? and domain = ? ";
		
		SerialNO snItem;
		List<?> list = commonService.getList(hql, _day, precode, domain);
		if(list.isEmpty()) {
			snItem = new SerialNO();
			snItem.setDay( _day );
			snItem.setPrecode(precode);
			snItem.setLastNum(0);
			
			commonService.createWithLog(snItem);
			snItem.setDomain(domain);
		} 
		else {
			snItem = (SerialNO) list.get(0);
		}
		
		List<String> result = new ArrayList<String>();
		count = Math.min(Math.max(1, count), 100000); // 单次最多1到100000个
		for(int i = 1; i <= count; i++) {
			int no = snItem.getLastNum() + i;
			
			String sn;
			if(snMode.length() == 0) {
				sn = "00" + no;
				sn = sn.substring(sn.length() - (no >= 1000 ? String.valueOf(no).length() : 3));
				
			} else {
				sn = "000" + no;
				sn = sn.substring(sn.length() - (no >= 10000 ? String.valueOf(no).length() : 4));
			}
			
			sn = precode + snMode + sn;
			result.add(sn);
		}
		
		int lastNum = snItem.getLastNum() + count;
		snItem.setLastNum(lastNum);
		commonService.update(snItem);
		
		return result;
	}

	public synchronized String createOne(String sntemplate) {
		return this.create(sntemplate, 1).get(0);
	}
	
	// 无限定前缀
	public static String get() {
		return get( _Field.SNO_yyMMddxxxx );
	}
	
	public static String get(String preCode) {
		return get(preCode, false);
	}
	
	public static String get(String preCode, boolean isGlobal) {
		return new SerialNOer(isGlobal).createOne( preCode );
	}
}
