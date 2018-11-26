/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.syncdata;

import java.util.Map;
 
public interface ISyncService {

    /**
     * 获取完全同步组时候需要用到的数据
     * 
     * @param mainGroupId   
     *              选中进行同步的组ID
     * @return
     */
    Map<String, Object> getCompleteSyncGroupData(Long mainGroupId);
 

}
