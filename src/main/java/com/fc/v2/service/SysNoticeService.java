package com.fc.v2.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fc.v2.common.base.BaseService;
import com.fc.v2.common.support.ConvertUtil;
import com.fc.v2.mapper.auto.SysNoticeMapper;
import com.fc.v2.mapper.auto.SysNoticeUserMapper;
import com.fc.v2.mapper.custom.NoticeDao;
import com.fc.v2.model.auto.SysNotice;
import com.fc.v2.model.auto.SysNoticeExample;
import com.fc.v2.model.auto.SysNoticeUser;
import com.fc.v2.model.auto.SysNoticeUserExample;
import com.fc.v2.model.auto.SysNoticeUserExample.Criteria;
import com.fc.v2.model.auto.TsysUser;
import com.fc.v2.model.auto.TsysUserExample;
import com.fc.v2.model.custom.Tablepar;
import com.fc.v2.satoken.SaTokenUtil;
import com.fc.v2.util.SnowflakeIdWorker;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

/**
 * 公告 SysNoticeService
 * @Title: SysNoticeService.java
 * @Package com.fc.v2.service
 * @author fuce_自动生成
 * @email 115889198@qq.com
 * @date 2019-09-08 01:38:44
 **/
@Service
public class SysNoticeService implements BaseService<SysNotice, SysNoticeExample>{
	@Autowired
	private SysNoticeMapper sysNoticeMapper;
	@Autowired
	private SysUserService sysUserService;
	@Autowired
	private SysNoticeUserService sysNoticeUserService;
	@Autowired
	private SysNoticeUserMapper sysNoticeUserMapper;
	@Autowired
	private NoticeDao noticeDao;
	/**
	 * 分页查询
	 * @param pageNum
	 * @param pageSize
	 * @return
	 */
	 public PageInfo<SysNotice> list(Tablepar tablepar,String name){
	        SysNoticeExample testExample=new SysNoticeExample();
	        testExample.setOrderByClause("id ASC");
	        if(name!=null&&!"".equals(name)){
	        	testExample.createCriteria().andTitleLike("%"+name+"%");
	        }

	        PageHelper.startPage(tablepar.getPage(), tablepar.getLimit());
	        List<SysNotice> list= sysNoticeMapper.selectByExample(testExample);
	        PageInfo<SysNotice> pageInfo = new PageInfo<SysNotice>(list);
	        return  pageInfo;
	 }
	 
	 
	 
 	/**
	 * 对应用户的所有公告信息
	 * @param pageNum
	 * @param pageSize
	 * @return
	 */
	 public PageInfo<SysNotice> list(TsysUser tsysUser,Tablepar tablepar,String name){
	        //查询未阅读的公告用户外键
			SysNoticeUserExample sysNoticeUserExample=new SysNoticeUserExample();
			Criteria criteria= sysNoticeUserExample.createCriteria();
			criteria.andUserIdEqualTo(tsysUser.getId());
			List<SysNoticeUser> noticeUsers= sysNoticeUserMapper.selectByExample(sysNoticeUserExample);
			if(noticeUsers!=null&&noticeUsers.size()>0) {
				//查询对应的公告列表
				List<String> ids=new ArrayList<String>();
				for (SysNoticeUser sysNoticeUser : noticeUsers) {
					ids.add(sysNoticeUser.getNoticeId());
				}
				
				//分页查询对应用户的所有公告信息
				SysNoticeExample testExample=new SysNoticeExample();
		        testExample.setOrderByClause("id desc");
		        com.fc.v2.model.auto.SysNoticeExample.Criteria criteria1= testExample.createCriteria();
		        if(name!=null&&!"".equals(name)){
		        	criteria1.andTitleLike("%"+name+"%");
		        }
			     
		        criteria1.andIdIn(ids);
		        criteria1.andStatusNotEqualTo(2);
		        PageHelper.startPage(tablepar.getPage(), tablepar.getLimit());
		        List<SysNotice> list= sysNoticeMapper.selectByExample(testExample);
		       
		        PageInfo<SysNotice> pageInfo = new PageInfo<SysNotice>(list);
				
				 return  pageInfo;
			}
			
			return new PageInfo<SysNotice>();
	      
	      
	 }
	 
	 
	@Override
	public int deleteByPrimaryKey(String ids) {
		List<String> lista=ConvertUtil.toListStrArray(ids);
		SysNoticeExample example=new SysNoticeExample();
		example.createCriteria().andIdIn(lista);
		return sysNoticeMapper.deleteByExample(example);
	}
	
	
	@Override
	public SysNotice selectByPrimaryKey(String id) {
		
		return sysNoticeMapper.selectByPrimaryKey(id);
	}

	
	@Override
	public int updateByPrimaryKeySelective(SysNotice record) {
		return sysNoticeMapper.updateByPrimaryKeySelective(record);
	}
	
	/**
	 * 添加公告（支持定向发布）
	 */
	@Override
	@Transactional
	public int insertSelective(SysNotice record) {
		//添加雪花主键id
		record.setId(SnowflakeIdWorker.getUUID());
		//添加创建人id
		record.setCreateId(SaTokenUtil.getUserId());
		//添加创建人
		record.setCreateUsername(SaTokenUtil.getLoginName());
		//添加创建时间
		record.setCreateTime(new Date());
		//默认状态：已发布
		if (record.getStatus() == null) { record.setStatus(1); }
		//默认范围：全部用户
		if (record.getScope() == null) { record.setScope(0); }

		sysNoticeMapper.insertSelective(record);

		//已发布状态才生成接收人快照
		if (record.getStatus() == 1) {
			List<String> userIds = resolveTargetUserIds(record.getScope(), record.getTargetIds());
			for (String userId : userIds) {
				SysNoticeUser noticeUser = new SysNoticeUser(null, record.getId(), userId, 0, null);
				sysNoticeUserService.insertSelective(noticeUser);
			}
		}
		return 1;
	}

	/**
	 * 根据发送范围解析目标用户ID列表（快照策略）
	 */
	private List<String> resolveTargetUserIds(int scope, String targetIds) {
		switch (scope) {
			case 0: // ALL
				List<TsysUser> allUsers = sysUserService.selectByExample(new TsysUserExample());
				List<String> allIds = new ArrayList<>();
				for (TsysUser u : allUsers) { allIds.add(u.getId()); }
				return allIds;
			case 1: // BY_ROLE
				return noticeDao.selectUserIdsByRoleIds(ConvertUtil.toListStrArray(targetIds));
			case 2: // BY_DEPT
				String[] arr = targetIds.split(",");
				List<Integer> deptIds = new ArrayList<>();
				for (String s : arr) { deptIds.add(Integer.parseInt(s.trim())); }
				return noticeDao.selectUserIdsByDeptIds(deptIds);
			case 3: // BY_USER
				return ConvertUtil.toListStrArray(targetIds);
			default:
				return new ArrayList<>();
		}
	}
	
	
	@Override
	public int updateByExampleSelective(SysNotice record, SysNoticeExample example) {
		
		return sysNoticeMapper.updateByExampleSelective(record, example);
	}

	
	@Override
	public int updateByExample(SysNotice record, SysNoticeExample example) {
		
		return sysNoticeMapper.updateByExample(record, example);
	}

	@Override
	public List<SysNotice> selectByExample(SysNoticeExample example) {
		
		return sysNoticeMapper.selectByExample(example);
	}

	
	@Override
	public long countByExample(SysNoticeExample example) {
		
		return sysNoticeMapper.countByExample(example);
	}

	
	@Override
	public int deleteByExample(SysNoticeExample example) {
		
		return sysNoticeMapper.deleteByExample(example);
	}
	
	/**
	 * 检查name
	 * @param sysNotice
	 * @return
	 */
	public int checkNameUnique(SysNotice sysNotice){
		SysNoticeExample example=new SysNoticeExample();
		example.createCriteria().andTitleEqualTo(sysNotice.getTitle());
		List<SysNotice> list=sysNoticeMapper.selectByExample(example);
		return list.size();
	}
	
	/**
	 * 获取用户未阅读公告
	 * @param tsysUser
	 * @param state 阅读状态  0未阅读 1 阅读  -1全部
	 * @return
	 * @author fuce
	 * @Date 2019年9月8日 上午3:36:21
	 */
	public List<SysNotice> getuserNoticeNotRead(TsysUser tsysUser,int state){
		List<SysNotice> notices=new ArrayList<>();
		//查询未阅读的公告用户外键
		SysNoticeUserExample sysNoticeUserExample=new SysNoticeUserExample();
		Criteria criteria= sysNoticeUserExample.createCriteria();
		criteria.andUserIdEqualTo(tsysUser.getId());
		if(-1!=state) {
			criteria.andStateEqualTo(state);
		}
		List<SysNoticeUser> noticeUsers= sysNoticeUserMapper.selectByExample(sysNoticeUserExample);
		if(noticeUsers!=null&&noticeUsers.size()>0) {
			//查询对应的公告列表
			List<String> ids=new ArrayList<String>();
			for (SysNoticeUser sysNoticeUser : noticeUsers) {
				ids.add(sysNoticeUser.getNoticeId());
			}
			SysNoticeExample noticeExample = new SysNoticeExample();
			noticeExample.createCriteria().andIdIn(ids).andStatusNotEqualTo(2);
			notices=sysNoticeMapper.selectByExample(noticeExample);
		}
		return notices;
	}
	
	
	/**
	 * 撤回公告（仅已发布状态可撤回）
	 */
	@Transactional
	public int recallNotice(String noticeId) {
		SysNotice notice = sysNoticeMapper.selectByPrimaryKey(noticeId);
		if (notice == null || notice.getStatus() == null || notice.getStatus() != 1) {
			return 0;
		}
		SysNotice update = new SysNotice();
		update.setId(noticeId);
		update.setStatus(2);
		return sysNoticeMapper.updateByPrimaryKeySelective(update);
	}

	/**
	 * 根据公告id把当前用户的公告置为已查看（幂等，撤回公告不可读）
	 * @param noticeid
	 */
	public void editUserState(String noticeid) {
		//撤回的公告不可标记已读
		SysNotice notice = sysNoticeMapper.selectByPrimaryKey(noticeid);
		if (notice != null && notice.getStatus() != null && notice.getStatus() == 2) {
			return;
		}
		SysNoticeUserExample sysNoticeUserExample=new SysNoticeUserExample();
		sysNoticeUserExample.createCriteria().andNoticeIdEqualTo(noticeid).andUserIdEqualTo(SaTokenUtil.getUserId());
		List<SysNoticeUser> noticeUsers= sysNoticeUserMapper.selectByExample(sysNoticeUserExample);
		for (SysNoticeUser sysNoticeUser : noticeUsers) {
			//幂等：仅未读状态才更新
			if (sysNoticeUser.getState() == 0) {
				sysNoticeUser.setState(1);
				sysNoticeUser.setReadTime(new Date());
				sysNoticeUserMapper.updateByPrimaryKey(sysNoticeUser);
			}
		}
	}

	/**
	 * 获取用户未读公告数量
	 */
	public int getUnreadCount(String userId) {
		return noticeDao.countUnreadByUserId(userId);
	}

	/**
	 * 获取最新8条公告
	 * @return
	 */
	public List<SysNotice>  getNEW(){
	        SysNoticeExample testExample=new SysNoticeExample();
	        testExample.setOrderByClause("id DESC");
	        PageHelper.startPage(1, 8);
	        List<SysNotice> list= sysNoticeMapper.selectByExample(testExample);
	        return  list;
	 }


}
