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
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.ddl._Database;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.ICommonDao;
import com.boubei.tss.framework.persistence.pagequery.MacrocodeQueryCondition;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.um.helper.dto.OperatorDTO;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.um.service.IMessageService;
import com.boubei.tss.util.EasyUtils;

/**
 * TODO 
 * 1、审批人禁止查看自己无审批权限的记录附件（审批角色对流程表有浏览权限）
 */
@Service("WFService")
public class WFServiceImpl implements WFService {
	
	protected Logger log = Logger.getLogger(this.getClass());
	
	@Autowired ILoginService loginSerivce;
	@Autowired IMessageService msgService;
	@Autowired ICommonDao commonDao;
 
	public WFStatus getWFStatus(Long tableId, Long itemId) {
		List<?> list = commonDao.getEntities("from WFStatus where tableId = ? and itemId = ? ", tableId, itemId);
		return  list.isEmpty() ? null : (WFStatus)list.get(0);
	}
	
	public List<?> getTransList(Long recordId, Long itemId) {
		WFStatus wfStatus = getWFStatus(recordId, itemId);
		return getUserList(wfStatus.getTrans());
	}
	
	public void calculateWFStatus(Long itemId, _Database _db) {
		
		String wfDefine = _db.wfDefine;
		if( !WFUtil.checkWorkFlow(wfDefine) ) return;
		
		Map<String, Object> item = _db.get(itemId);
		
		/* 如果是修改记录，则先查询流程状态是否已经存在；如果已存在，检查是否已有过处理（含审批、驳回、转审），有则禁止修改 */
		WFStatus wfStatus = getWFStatus(_db.recordId, itemId);
		if(wfStatus != null) {
			if( wfStatus.getProcessors() != null ) {
				throw new BusinessException(EX.WF_1); 
			}
		} else {
			wfStatus = new WFStatus();
			wfStatus.setTableName(_db.recordName);
			wfStatus.setTableId(_db.recordId);
			wfStatus.setItemId( itemId );
			wfStatus.setCurrentStatus(WFStatus.NEW);
			wfStatus.setApplier( Environment.getUserCode() );
			wfStatus.setApplierName( Environment.getUserName() );
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
		List<Map<String, String>> trans = rules.get("trans");
		
		List<String> tos = getUsers(to);
		List<String> ccs = getUsers(cc);
		List<String> transs = getUsers(trans);
		
		wfStatus.setNextProcessor( tos.get(0) );
		wfStatus.setStepCount( tos.size() );
		wfStatus.setTo( EasyUtils.list2Str(tos) );
		wfStatus.setCc( EasyUtils.list2Str(ccs) );
		wfStatus.setTrans(  EasyUtils.list2Str(transs)  );
		
		updateWFStatus(wfStatus);
	}
	
	/**
	 * 安装流程规则及当前申请人登录信息，获取审批人（或抄送人）列表
	 */
	public List<String> getUsers(List<Map<String, String>> rule) {
		List<String> users = new ArrayList<String>();
		if(rule == null) return users;
		
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
	private List<Object[]> getSameGroupUserByRole(Long roleId, String creator) {
		
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
		
		return result.subList(0, 1); // 只取第一个
	}
	
	/**
	 * 我审批的: 
	 * 		1、待审批的  nextProcessor = 我
	 * 		2、已审批的  processors.contains(我): 含已审批、已转审、已驳回）
	 * 		3、抄送给我的 cc.contains(我)
	 * 注：剔除已撤销的
	 */
	public SQLExcutor queryMyTasks(_Database _db, Map<String, String> params, int page, int pagesize) {
		String userCode = Environment.getUserCode(); 
    	String wrapCode = MacrocodeQueryCondition.wrapLike(userCode);
    	
    	/* 支持按类型分开查询: wfing: 待审批  wfdone: 已审批  cc: 抄送  */
		String _condition = "<#if wfing?? >  or nextProcessor = '" +userCode+ "' </#if> " +
				           "<#if wfdone?? > or processors like '" +wrapCode+ "' </#if> " +
				           "<#if wfcc?? >   or cc like '" +wrapCode+ "' </#if> ";
		
		String condition = EasyUtils.fmParse(_condition, params);
		if( EasyUtils.isNullOrEmpty(condition) ) {
			params.put("wfing", "true");
			params.put("wfdone", "true");
			params.put("wfcc", "true");
			condition = EasyUtils.fmParse(_condition, params);
		}
		String hsql = "from WFStatus where ( 1=0 " +condition+ " )  and tableId = ? and currentStatus <> ? "; 

		List<?> statusList = commonDao.getEntities(hsql, _db.recordId, WFStatus.CANCELED);

    	List<Long> itemIds = new ArrayList<Long>();
    	Map<Long, WFStatus> statusMap = new HashMap<Long, WFStatus>();
    	boolean isApprover = false; // 判断当前用户是否为审批人
    	
    	for(Object obj : statusList) {
    		WFStatus status = (WFStatus) obj;
    		Long itemId = status.getItemId();
			itemIds.add(itemId);
    		statusMap.put(itemId, status);
    		
    		if( status.toUsers().contains(userCode) ) {
    			isApprover = true;
    		}
    	}
    	itemIds.add(-999L); // 防止id条件为空把所有记录都查出来了
    	params.put("id", EasyUtils.list2Str(itemIds));
    	
    	SQLExcutor ex = _db.select(page, pagesize, params, isApprover);
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
	
	private List<Map<String, Object>> getUserList(String userCodes) {
		userCodes += ",noThis";
		String sql = "select username, loginName usercode from um_user where loginName in (" +DMUtil.insertSingleQuotes(userCodes)+ ")";
		return SQLExcutor.queryL(sql);
	}

	public void appendWFInfo(_Database _db, Map<String, Object> item, Long itemId) {
		if( !WFUtil.checkWorkFlow(_db.wfDefine) ) return;
		
		// 流程状态
		WFStatus wfStatus = getWFStatus(_db.recordId, itemId);
		String processors = EasyUtils.obj2String( wfStatus.getProcessors() );
		item.put("wfstatus", wfStatus.getCurrentStatus());
		item.put("nextProcessor", wfStatus.getNextProcessor());
		item.put("processors", processors);
		item.put("applier", wfStatus.getApplierName() );
		
		// 抄送人列表（中文姓名）
		String cc = wfStatus.getCc();
		item.put("cc_list", EasyUtils.attr2Str(getUserList(cc), "username"));
		
		// 流程日志
		@SuppressWarnings("unchecked")
		List<WFLog> logs = (List<WFLog>) commonDao.getEntities("from WFLog where tableId = ? and itemId = ? order by id desc", _db.recordId, itemId);
		
		// 根据to审批人列表，模拟出审批步骤
		String[] toList = wfStatus.getTo().split(",");
		for( String to : toList ) {
			if( !wfStatus.processorList().contains(to) ) {
				WFLog log = new WFLog();
				log.setProcessor( EasyUtils.attr2Str(getUserList(to), "username") );
				log.setProcessResult(WFStatus.UNAPPROVE);
				
				logs.add(log);
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
    	updateWFStatus(wfStatus);
    	
    	WFLog wfLog = new WFLog(wfStatus, opinion);
    	wfLog.setProcessResult( WFStatus.APPROVED );
		commonDao.createObject(wfLog);
	}
	
	public void reject(Long recordId, Long id, String opinion) {

		WFStatus wfStatus = setWFStatus(recordId, id, false, false);
  
    	wfStatus.setCurrentStatus(WFStatus.REJECTED);
    	wfStatus.setNextProcessor(null);
    	updateWFStatus(wfStatus);
    	
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
    	updateWFStatus(wfStatus);
    	
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
    	updateWFStatus(wfStatus);
    	
		WFLog wfLog = new WFLog(wfStatus, opinion);
		commonDao.createObject(wfLog);
	}
	
	private void updateWFStatus(WFStatus wfStatus) {
		commonDao.update(wfStatus);
		
		String tableName = wfStatus.getTableName();
		Long tableId = wfStatus.getTableId();
		Long itemId = wfStatus.getItemId();
		
		String url = "javascript:void(0)"; // "'/tss/modules/dm/recorder.html?id=" +tableId+ "&itemId=" +itemId+ "' target='_blank'";
		String onclick = "parent.openUrl('more/bi_nav.html?_default=" +tableId+ "&_defaultItem=" +itemId+ "')";
		
		// 分别给流程发起人及下一步处理人，发送站内信、邮件、短信等通知
		if( !Environment.getUserCode().equals(wfStatus.getApplier()) ) {
			String title = "您提交的流程【" + tableName + "】" + wfStatus.getCurrentStatus();
			String content = title + "，<a href=\"" +url+ "\" onclick=\"" +onclick+ "\">查看最新进度</a>";
			msgService.sendMessage(title, content, wfStatus.getApplier());
		}
		
		String title = "有新的流程【" + tableName + "】待您审批";
		String content = title + "，<a href=\"" +url+ "\" onclick=\"" +onclick+ "\">点击打开处理</a>";
		msgService.sendMessage(title, content, wfStatus.getNextProcessor());
	}
}