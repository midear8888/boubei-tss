package com.boubei.tss.dm.record.workflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.EX;
import com.boubei.tss.dm.ddl._Database;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.ICommonDao;
import com.boubei.tss.framework.persistence.pagequery.MacrocodeQueryCondition;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.um.helper.dto.OperatorDTO;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.util.EasyUtils;

@Service("WFService")
public class WFServiceImpl implements WFService {
	
	protected Logger log = Logger.getLogger(this.getClass());
	
	@Autowired ILoginService loginSerivce;
	@Autowired ICommonDao commonDao;
 
	public WFStatus getWFStatus(Long tableId, Long itemId) {
		List<?> list = commonDao.getEntities("from WFStatus where tableId = ? and itemId = ? ", tableId, itemId);
		return  list.isEmpty() ? null : (WFStatus)list.get(0);
	}
	
	public void calculateWFStatus(Long itemId, _Database _db) {
		
		String wfDefine = _db.wfDefine;
		if( !WFUtil.checkWorkFlow(wfDefine) ) return;
		
		Map<String, Object> item = _db.get(itemId);
		
		/* 如果是修改记录，则先查询流程状态是否已经存在；如果已存在，检查是否已有过处理（含审批、驳回、转审），有则禁止修改 */
		WFStatus wfStatus = getWFStatus(_db.recordId, itemId);
		if(wfStatus != null) {
			if( wfStatus.getProcessors() != null ) {
				throw new BusinessException(EX.WF_1); // TODO 附件修改也要禁止
			}
		} else {
			wfStatus = new WFStatus();
			wfStatus.setTableId(_db.recordId);
			wfStatus.setItemId( itemId );
			wfStatus.setCurrentStatus(WFStatus.NEW);
			wfStatus.setApplier( Environment.getUserCode() );
			commonDao.create(wfStatus);
		}
		
		// 把提交人的信息也作为规则运算条件
		Map<String, Object> context = new HashMap<String, Object>( item );
		context.put("userCode", Environment.getUserCode());
		context.put("userName", Environment.getUserName());
		context.put("group", Environment.getInSession("GROUP_LAST_NAME"));
		for( Long role : Environment.getOwnRoles() ) {
			context.put("role_" + role, role);
		}
		for( String role : Environment.getOwnRoleNames() ) {
			context.put( role, role );
		}
		
		Map<String, List<Map<String, String>>> rules = WFUtil.parseWorkflow(wfDefine, context, _db.recordName);
		
		List<Map<String, String>> to = rules.get("to");
		List<Map<String, String>> cc = rules.get("cc");
		
		List<String> tos = getUsers(to);
		List<String> ccs = getUsers(cc);
		
		wfStatus.setNextProcessor( tos.get(0) );
		wfStatus.setStepCount( tos.size() );
		wfStatus.setTo( EasyUtils.list2Str(tos) );
		wfStatus.setCc( EasyUtils.list2Str(ccs) );
		
		commonDao.update(wfStatus);
	}
	
	/**
	 * 安装流程规则及当前申请人登录信息，获取审批人（或抄送人）列表
	 */
	public List<String> getUsers(List<Map<String, String>> rule) {
		List<String> users = new ArrayList<String>();
		
		String creator = Environment.getUserCode(); 
		
		for(Map<String, String> m : rule) {
			
			String user = m.get("user"); // 用户需要配置在域扩展表里，使用loginName
			if( !EasyUtils.isNullOrEmpty(user) ) {
				users.add( user );
			}
			
			String role = m.get("roleId"); // 或签名，优先使用和申请人同组的主管
			if( !EasyUtils.isNullOrEmpty(role) ) {
				Long roleId = EasyUtils.obj2Long(role);
				List<Object[]> roleUsers = getSameGroupUserByRole(roleId, creator);
				if( roleUsers.size() > 0 ) {
					users.addAll( Arrays.asList( EasyUtils.list2Str(roleUsers, 0).split(",") ) );
				}
			}
		}
 
		return users;
	}
	
	/**
	 * 按角色获取拥有此角色的所有用户 及 组织
	 */
	public List<Object[]> getSameGroupUserByRole(Long roleId, String creator) {
		
		List<Object[]> result = new ArrayList<Object[]>();
		if( Environment.getOwnRoles().contains(roleId) ) { // 如果自己就拥有该角色，则无需此角色的人审批
			return result;
		}
		
		Long inGroup = (Long) Environment.getInSession("GROUP_LAST_ID");
		List<OperatorDTO> list = loginSerivce.getUsersByRoleId(roleId);
		
		for(OperatorDTO dto : list) {
			String userCode = dto.getLoginName();
			
			result.add( new String[]{ userCode, dto.getUserName()} );
			
			List<Object[]> fatherGroups = loginSerivce.getGroupsByUserId(dto.getId());
	        for( int i = fatherGroups.size() - 1; i >= 0; i--) {
	        	Object[] temp = fatherGroups.get(i);
	        	if( inGroup.equals(temp[0]) ) {
	        		return result.subList(result.size() - 1, result.size() ); // 取最后一个
	        	}
	        }
		}
		
		return result;
	}
	
	/**
	 * 我审批的 = 待审批的（nextProcessor = 我） + 已审批的（processors.contains(我): 含已审批、已转审、已驳回）+ 抄送给我的（cc.contains(我)）
	 */
	public SQLExcutor queryMyTasks(_Database _db, Map<String, String> params, int page, int pagesize) {

		Long recordId = _db.recordId;
    	String userCode = Environment.getUserCode(); 
    	String wrapCode = MacrocodeQueryCondition.wrapLike(userCode);
		String hsql = "from WFStatus where tableId = ? and ( ? in (nextProcessor) or processors like ? or cc like ?) "; // ? in (nextProcessor, applier)
		List<?> statusList = commonDao.getEntities(hsql, 
    			recordId, userCode, wrapCode, wrapCode);

    	List<Long> itemIds = new ArrayList<Long>();
    	Map<Long, WFStatus> statusMap = new HashMap<Long, WFStatus>();
    	for(Object obj : statusList) {
    		WFStatus status = (WFStatus) obj;
    		Long itemId = status.getItemId();
			itemIds.add(itemId);
    		statusMap.put(itemId, status);
    	}
    	itemIds.add(-999L); // 防止id条件为空把所有记录都查出来了
    	params.put("id", EasyUtils.list2Str(itemIds));
    	
    	SQLExcutor ex = _db.select(page, pagesize, params);
		List<Map<String, Object>> items = ex.result;
    	
    	// 加上每一行的流程状态： 审批中、已通过、已撤销、已驳回、已转审
    	for(Map<String, Object> item : items) {
    		Object itemId = item.get("id");
			WFStatus wfStatus = statusMap.get(itemId);
			item.put("wfstatus", wfStatus.getCurrentStatus());
			item.put("nextProcessor", wfStatus.getNextProcessor());
			item.put("processors", wfStatus.getProcessors());
    	}
    	
    	return ex;
	}
	
	public void fixWFStatus(_Database _db, List<Map<String, Object>> items) {
		if( !WFUtil.checkWorkFlow(_db.wfDefine) ) return;
		
		List<Long> itemIds = new ArrayList<Long>();
		Map<Long, Map<String, Object>> itemsMap = new HashMap<Long, Map<String, Object>>();
		for(Map<String, Object> item : items) {
			Long itemId = (Long) item.get("id");
			itemIds.add( itemId );
			itemsMap.put(itemId, item);
		}
		itemIds.add(-999L);
		
		List<?> statusList = commonDao.getEntities("from WFStatus where tableId = ? and itemId in (" +EasyUtils.list2Str(itemIds)+ ") ", _db.recordId);
    	
    	for(Object obj : statusList) {
    		WFStatus status = (WFStatus) obj;
    		itemsMap.get( status.getItemId() ).put("wfstatus", status.getCurrentStatus());
    	}
	}

	public void appendWFInfo(_Database _db, Map<String, Object> item, Long itemId) {
		if( !WFUtil.checkWorkFlow(_db.wfDefine) ) return;
		
		// 流程状态
		WFStatus wfStatus = getWFStatus(_db.recordId, itemId);
		String processors = EasyUtils.obj2String( wfStatus.getProcessors() );
		item.put("wfstatus", wfStatus.getCurrentStatus());
		item.put("nextProcessor", wfStatus.getNextProcessor());
		item.put("processors", processors);
		
		// 流程日志
		List<?> logs = commonDao.getEntities("from WFLog where tableId = ? and itemId = ? order by id desc", _db.recordId, itemId);
		
		// 根据to审批人列表，模拟出审批步骤
		String[] toList = wfStatus.getTo().split(",");
		for( String to : toList ) {
			if( !wfStatus.processorList().contains(to) ) {
				WFLog log = new WFLog();
				log.setProcessor(to);
				log.setProcessResult(WFStatus.UNAPPROVE);
			}
		}
		
		item.put("wf_logs", logs);
	}
	
	private WFStatus setWFStatus(Long recordId, Long itemId, boolean isCancel, boolean isApprove) {
		
		String userCode = Environment.getUserCode();
		WFStatus wfStatus = this.getWFStatus(recordId, itemId);
		if( wfStatus.getNextProcessor() == null) {
    		throw new BusinessException(EX.WF_3);
        }
		
		wfStatus.currStepIndex = wfStatus.toUsers().indexOf(userCode);
    	if( wfStatus.currStepIndex < 0 ) {
    		if( !isCancel || !userCode.equals(wfStatus.getApplier()) ) {
    			throw new BusinessException(EX.WF_4);
    		} 
    	}
    	
    	if( isApprove && !Environment.getUserCode().equals(wfStatus.getNextProcessor()) ) {
    		throw new BusinessException(EX.WF_4); 
        }
    	
    	wfStatus.setLastProcessor(Environment.getUserName());
    	
    	String processors = EasyUtils.obj2String(wfStatus.getProcessors()) + "," + userCode;
    	if(processors.startsWith(",")) {
    		processors = processors.substring(1);
    	}
		wfStatus.setProcessors( processors );
    	
    	return wfStatus;
	}
	
	public void approve(Long recordId, Long itemId, String opinion) {

		WFStatus wfStatus = setWFStatus(recordId, itemId, false, true);
		String processors = wfStatus.getProcessors();
		
		String currentStatus, nextProcessor;
    	if( processors.split(",").length == wfStatus.getStepCount() ) {
    		currentStatus = WFStatus.PASSED;
    		nextProcessor = null;
    	} else {
    		currentStatus = WFStatus.APPROVING;
    		nextProcessor = wfStatus.toUsers().get(wfStatus.currStepIndex + 1);
    	}
    	wfStatus.setCurrentStatus(currentStatus);
    	wfStatus.setNextProcessor(nextProcessor);
    	commonDao.update(wfStatus);
    	
    	WFLog wfLog = new WFLog(wfStatus, opinion);
		commonDao.createObject(wfLog);
	}
	
	public void reject(Long recordId, Long id, String opinion) {

		WFStatus wfStatus = setWFStatus(recordId, id, false, false);
  
    	wfStatus.setCurrentStatus(WFStatus.REJECTED);
    	wfStatus.setNextProcessor(null);
    	commonDao.update(wfStatus);
    	
		WFLog wfLog = new WFLog(wfStatus, opinion);
		commonDao.createObject(wfLog);
	}
	
	public void transApprove(Long recordId, Long id, String opinion, String target) {
		
		if( EasyUtils.isNullOrEmpty(target) ) {
			throw new BusinessException(EX.WF_5);
		}

		WFStatus wfStatus = setWFStatus(recordId, id, false, false);
  
    	wfStatus.setCurrentStatus(WFStatus.TRANS);
    	wfStatus.setNextProcessor(target);
    	wfStatus.setTo( wfStatus.getTo().replaceAll(Environment.getUserCode(), Environment.getUserCode() + "," + target) );
    	wfStatus.setStepCount( wfStatus.getStepCount() + 1 );
    	commonDao.update(wfStatus);
    	
		WFLog wfLog = new WFLog(wfStatus, opinion);
		commonDao.createObject(wfLog);
	}
	
	public void cancel(Long recordId, Long id, String opinion) {

		WFStatus wfStatus = setWFStatus(recordId, id, true, false);
		if( !WFStatus.NEW.equals(wfStatus.getCurrentStatus()) ) {
			throw new BusinessException(EX.WF_2);
		}
  
    	wfStatus.setCurrentStatus(WFStatus.CANCELED);
    	wfStatus.setNextProcessor(null);
    	commonDao.update(wfStatus);
    	
		WFLog wfLog = new WFLog(wfStatus, opinion);
		commonDao.createObject(wfLog);
	}
}
