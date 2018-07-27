/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.service;

import java.util.Collection;
import java.util.List;

import com.boubei.tss.cache.aop.Cached;
import com.boubei.tss.cache.extension.CacheLife;
import com.boubei.tss.um.helper.dto.GroupDTO;
import com.boubei.tss.um.helper.dto.OperatorDTO;

/**
 * <p>
 * 用户登录系统相关业务逻辑处理接口：
 * <li>根据用户登录名获取用户名及认证方式信息等；
 * <li>根据用户ID获取用户信息；
 * <li>根据用户登录名获取用户信息；
 * </p>
 */
public interface ILoginService {

    /**
     * <p>
     * 根据用户登录名获取用户名及身份认证器类名
     * </p>
     * @param loginName 用户登录名
     * @return String[] {用户名:String, 身份认证器类名（全路径）:String}
     */
	@Cached(cyclelife = CacheLife.SHORT)
    String[] getLoginInfoByLoginName(String loginName);
	
	/**
	 * 统计用户连续输错密码的次数
	 * @param loginName
	 */
	int checkPwdErrorCount(String loginName);
	void recordPwdErrorCount(String loginName, int currCount);
	
	/**
	 * 设置用户最后登录系统的时间
	 */
	void setLastLoginTime(Long userId);
    
	/**
	 * 重新设置密码。
	 * 
	 * @param userId
	 * @param password
	 */
	Object resetPassword(Long userId, String password);
	
	int checkPwdSecurity(Long userId);
	
    /**
     * <p>
     * 根据用户ID获取用户信息
     * </p>
     * @param id 用户ID
     * @return OperatorDTO 用户信息DTO
     */
    OperatorDTO getOperatorDTOByID(Long id);
    
    /**
     * <p>
     * 根据用户登录名获取用户信息
     * </p>
     * @param loginName 用户登录名
     * @return OperatorDTO 用户信息DTO
     */
    OperatorDTO getOperatorDTOByLoginName(String loginName);
    
    /**
     * <p>
     * 登陆成功后，保存【用户对应角色列表】到RoleUserMapping表，同时删除旧的关系
     * </p>
     * @param logonUserId 
     */
    List<Long> saveUserRolesAfterLogin(Long logonUserId);

    /**
     * <p>
     * 获取登陆用户的所有的角色列表
     * 
     * 注：不宜缓存，多线程时容易ConcurrentModificationException
     * </p>
     * @param userId
     *          用户ID
     * @return
     *      登陆用户拥有的所有权限 List(roleId)
     */
    List<Long> getRoleIdsByUserId(Long userId);
    
    List<String> getRoleNames(Collection<Long> roleIds);
    
    @Cached(cyclelife = CacheLife.SHORT)
    List<Object[]> getAssistGroupIdsByUserId(Long userId);

    /**
     * 根据用户获取用户所在组织关系
     * @param userId
     * @return
     * 		List: Object[](groupId, groupName)
     *		层次是从上向下,依次类推
     */
    @Cached(cyclelife = CacheLife.SHORT)
    List<Object[]> getGroupsByUserId(Long userId);

    /**
     * <p>
     * 根据组id获取该组下的儿子结点。（供远程调用，需要转换成GroupDTO，Group只限于TSS使用）
     * 其它基于TSS的应用需要取部门列表的话可以采用本方法获取。
     * </p>
     * @param groupId
     * @return
     */
    List<GroupDTO> getGroupTreeByGroupId(Long groupId);
    
    /**
     * 取组（不包含子组）下的用户列表，转换为OperatorDTO对象列表。
     * @param groupId
     * @return
     */
    List<OperatorDTO> getUsersByGroupId(Long groupId);
 
    /**
     * 根据角色的ID 获取拥有此角色的用户
     * 注：需要同时取出“转授”关联起来的RoleUser。
     * @param roleId
     * @return List
     */
    List<OperatorDTO> getUsersByRoleId(Long roleId);
    
    /**
     * 查找指定域下的所有用户，按【域 + 登录用户】进行缓存（开发者域的域名相同，需要用logonUserId来区分是哪个开发者）
     * 
     * @param domain
     * @param field
     * @return
     */
    @Cached(cyclelife = CacheLife.SHORT)
    List<?> getUsersByDomain(String domain, String field, Long logonUserId);
    
	/**
	 * 读取用户联系方式：
	 * 1、ID列表，用于站内消息
	 * 2、邮件列表，支持loginName，email，角色，用户组，辅助组、参数宏。
	 * 如： lovejava@163.com,BL01037,-1@tssRole,-2@tssGroup,${JK}
	 * 
     * @param receiverStr
     * @param justID  站内信按用户ID发送
     * @return
     */
    String[] getContactInfos(String receiverStr, boolean justID);
}

