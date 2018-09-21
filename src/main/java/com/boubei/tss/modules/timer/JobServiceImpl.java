package com.boubei.tss.modules.timer;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.EX;
import com.boubei.tss.dm.etl.AbstractETLJob;
import com.boubei.tss.dm.etl.Task;
import com.boubei.tss.dm.etl.TaskLog;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.ICommonDao;
import com.boubei.tss.modules.log.IBusinessLogger;
import com.boubei.tss.util.BeanUtil;

@Service("JobService")
public class JobServiceImpl implements JobService {
	
	@Autowired ICommonDao commonDao;
	@Autowired IBusinessLogger businessLogger;
	
	public String excuteJob(String jobKey, Object tag) {
		List<?> list = commonDao.getEntities("from JobDef where ? in (id, code) and disabled <> 1 ", jobKey);
		if( list.isEmpty() ) {
			return EX.EXCEPTION;
		}
		
		JobDef jobDef = (JobDef) list.get(0);
		String jobClass = jobDef.getJobClassName();
		
		AbstractJob job = (AbstractJob) BeanUtil.newInstanceByName(jobClass);
		String resultMsg = job.excuting(jobDef.getCode(), jobDef.getCustomizeInfo(), jobDef.getId() );
		
		return resultMsg;
	}

	public String excuteTask(Long taskId, Object tag) {
		Task task = (Task) commonDao.getEntity(Task.class, taskId);
		JobDef jobDef = (JobDef) commonDao.getEntity(JobDef.class, task.getJobId());
		String jobClass = jobDef.getJobClassName();
		
		Object job = BeanUtil.newInstanceByName(jobClass);
		if( job instanceof AbstractETLJob ) {
			TaskLog log = ((AbstractETLJob)job).excuteTask(task);
			if( log == null ) {
				return EX.DM_30;
			}
			else if( "yes".equals( log.getException() ) ) {
				throw new BusinessException(task.getJobName() + EX._ERROR_TAG + log.getDetail() );
			}
		}
		else {
			throw new BusinessException( EX.parse(EX.DM_28, task.getJobId(), task.getJobName()) );
		}
		
		return EX.DEFAULT_SUCCESS_MSG;
	}

}
