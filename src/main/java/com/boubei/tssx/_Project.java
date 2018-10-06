package com.boubei.tssx;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.MathUtil;
import com.boubei.tss.util.URLUtil;

/**
 * 项目管理：需求、进度、质量、异常
 */
@Controller
@RequestMapping("/project")
public class _Project {
	
	/**
	 * http://localhost:9000/tss/pages/staff/codereview.html
	 * 
	 * http://localhost:9000/tss/init/codefile?pack=pages
	 * http://localhost:9000/tss/init/codefile?pack=com/boubei/scm
	 */
	@RequestMapping(value = "/codefile", method = RequestMethod.GET)
	@ResponseBody
	public Set<?> getCodeFiles(String pack, String type) {
		
		Map<Long, String> m = new HashMap<Long, String>();
		List<File> list = new ArrayList<File>();
		
		try {
			File dir = new File( URLUtil.getResourceFileUrl(pack).getPath() );  // pack=com/boubei/scm
			list = FileHelper.listFilesByTypeDeeply("class", dir);
			for(File file : list ) {
				Long key = 13000000000L + MathUtil.randomInt(999999999);
				String dirName = file.getParentFile().getName();
				if( !"entity".equals(dirName) )
					m.put(key, file.getName());
			}
		} catch(Exception e) {
		}
		
		File dir = new File( URLUtil.getWebFileUrl(pack).getPath() );
		if( type != null) {
			list = FileHelper.listFilesByTypeDeeply(type, dir);
		} else {
			list = FileHelper.listFilesByTypeDeeply("js", dir); 
			list.addAll( FileHelper.listFilesByTypeDeeply("css", dir) ); 
			list.addAll( FileHelper.listFilesByTypeDeeply("htm", dir) ); 
			list.addAll( FileHelper.listFilesByTypeDeeply("html", dir) ); 
		}
		
		for(File file : list ) {
			Long key = 13000000000L + MathUtil.randomInt(999999999);
			String dirName = file.getParentFile().getName();
			m.put(key, dirName + "/" + file.getName());
		}
		
		m.put(13000000000L + MathUtil.randomInt(999999999), "小程序框架");
		
		return m.entrySet();
	}

}
