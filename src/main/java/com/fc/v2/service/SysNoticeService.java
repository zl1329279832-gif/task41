package com.fc.v2.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import com.fc.v2.model.custom.NoticeReadStatVO;
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
	 * 管理员分页查询公告列表（显示所有状态）
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
	 * 对应用户的所有公告信息（排除已撤回）
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
			 // 排除已撤回的公告
			 criteria1.andStatusNotEqualTo(1);
			 criteria1.andIdIn(ids);
			 PageHelper.startPage(tablepar.getPage(), tablepar.getLimit());
			 List<SysNotice> list= sysNoticeMapper.selectByExample(testExample);

			 PageInfo<SysNotice> pageInfo = new PageInfo<SysNotice>(list);

			 return  pageInfo;
		 }

		 return new PageInfo<SysNotice>();


	 }


	@Override
	@Transactional
	public int deleteByPrimaryKey(String ids) {
		List<String> lista=ConvertUtil.toListStrArray(ids);
		// 先删除关联的 notice_user 记录
		SysNoticeUserExample userExample = new SysNoticeUserExample();
		userExample.createCriteria().andNoticeIdIn(lista);
		sysNoticeUserMapper.deleteByExample(userExample);
		// 再删除公告记录
		SysNoticeExample example=new SysNoticeExample();
		example.createCriteria().andIdIn(lista);
		return sysNoticeMapper.deleteByExample(example);
	}


	@Override
	public SysNotice selectByPrimaryKey(String id) {

		return sysNoticeMapper.selectByPrimaryKey(id);
	}


	@Override
	@Transactional
	public int updateByPrimaryKeySelective(SysNotice record) {
		// 加载旧记录，比较范围是否变更
		SysNotice existing = sysNoticeMapper.selectByPrimaryKey(record.getId());
		int result = sysNoticeMapper.updateByPrimaryKeySelective(record);

		// 如果提交了 scopeType，检查范围是否变化
		Integer newScopeType = record.getScopeType();
		if(newScopeType != null && existing != null) {
			Integer oldScopeType = existing.getScopeType() != null ? existing.getScopeType() : 0;
			String oldScopeIds = existing.getScopeIds();
			String newScopeIds = record.getScopeIds();

			boolean scopeChanged = !newScopeType.equals(oldScopeType);
			if(!scopeChanged && newScopeIds != null) {
				scopeChanged = !newScopeIds.equals(oldScopeIds != null ? oldScopeIds : "");
			}

			if(scopeChanged) {
				syncNoticeRecipients(record.getId(), newScopeType, newScopeIds);
			}
		}
		return result;
	}

	/**
	 * 同步公告接收人快照（diff 方式：删除多余、补入新增、保留已有的已读状态）
	 */
	private void syncNoticeRecipients(String noticeId, Integer scopeType, String scopeIds) {
		// 1. 查询现有接收人
		SysNoticeUserExample existingExample = new SysNoticeUserExample();
		existingExample.createCriteria().andNoticeIdEqualTo(noticeId);
		List<SysNoticeUser> existingRecords = sysNoticeUserMapper.selectByExample(existingExample);
		Set<String> existingUserIds = new HashSet<>();
		for(SysNoticeUser nu : existingRecords) {
			existingUserIds.add(nu.getUserId());
		}

		// 2. 解析新目标用户
		List<String> newTargetList = resolveTargetUserIds(scopeType, scopeIds);
		Set<String> newUserIds = new HashSet<>(newTargetList);

		// 3. 计算差异
		// 需要移除的：在旧集合但不在新集合
		Set<String> toRemove = new HashSet<>(existingUserIds);
		toRemove.removeAll(newUserIds);
		// 需要新增的：在新集合但不在旧集合
		Set<String> toAdd = new HashSet<>(newUserIds);
		toAdd.removeAll(existingUserIds);

		// 4. 删除不再属于目标范围的接收人记录
		if(!toRemove.isEmpty()) {
			SysNoticeUserExample deleteExample = new SysNoticeUserExample();
			deleteExample.createCriteria()
				.andNoticeIdEqualTo(noticeId)
				.andUserIdIn(new ArrayList<>(toRemove));
			sysNoticeUserMapper.deleteByExample(deleteExample);
		}

		// 5. 为新进入目标范围的用户创建记录
		if(!toAdd.isEmpty()) {
			List<SysNoticeUser> newRecords = new ArrayList<>();
			for(String userId : toAdd) {
				SysNoticeUser nu = new SysNoticeUser();
				nu.setId(SnowflakeIdWorker.getUUID());
				nu.setNoticeId(noticeId);
				nu.setUserId(userId);
				nu.setState(0);
				newRecords.add(nu);
			}
			int batchSize = 500;
			for(int i = 0; i < newRecords.size(); i += batchSize) {
				int end = Math.min(i + batchSize, newRecords.size());
				noticeDao.batchInsertNoticeUser(newRecords.subList(i, end));
			}
		}
	}

	/**
	 * 添加公告（支持定向发布）
	 * scopeType: 0-全部用户, 1-指定角色, 2-指定部门, 3-指定用户
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
		//设置状态为正常
		record.setStatus(0);
		//默认scopeType为0（全部用户）
		if(record.getScopeType() == null) {
			record.setScopeType(0);
		}

		sysNoticeMapper.insertSelective(record);

		// 根据 scopeType 解析目标用户列表
		List<String> targetUserIds = resolveTargetUserIds(record.getScopeType(), record.getScopeIds());

		// 批量创建 notice_user 记录（接收人快照）
		if(targetUserIds != null && !targetUserIds.isEmpty()) {
			List<SysNoticeUser> noticeUserList = new ArrayList<>();
			for (String userId : targetUserIds) {
				SysNoticeUser noticeUser = new SysNoticeUser();
				noticeUser.setId(SnowflakeIdWorker.getUUID());
				noticeUser.setNoticeId(record.getId());
				noticeUser.setUserId(userId);
				noticeUser.setState(0);
				noticeUserList.add(noticeUser);
			}
			// 分批插入，每批500条
			int batchSize = 500;
			for(int i = 0; i < noticeUserList.size(); i += batchSize) {
				int end = Math.min(i + batchSize, noticeUserList.size());
				noticeDao.batchInsertNoticeUser(noticeUserList.subList(i, end));
			}
		}
		return 1;
	}

	/**
	 * 根据scopeType和scopeIds解析目标用户ID列表
	 */
	private List<String> resolveTargetUserIds(Integer scopeType, String scopeIds) {
		if(scopeType == null || scopeType == 0) {
			// 全部用户
			List<TsysUser> allUsers = sysUserService.selectByExample(new TsysUserExample());
			List<String> userIds = new ArrayList<>();
			for(TsysUser u : allUsers) {
				userIds.add(u.getId());
			}
			return userIds;
		} else if(scopeType == 1) {
			// 指定角色
			List<String> roleIds = new ArrayList<>();
			for(String s : scopeIds.split(",")) {
				String trimmed = s.trim();
				if(!trimmed.isEmpty()) {
					roleIds.add(trimmed);
				}
			}
			return noticeDao.selectUserIdsByRoleIds(roleIds);
		} else if(scopeType == 2) {
			// 指定部门
			List<Integer> deptIds = new ArrayList<>();
			for(String s : scopeIds.split(",")) {
				String trimmed = s.trim();
				if(!trimmed.isEmpty()) {
					deptIds.add(Integer.parseInt(trimmed));
				}
			}
			return noticeDao.selectUserIdsByDeptIds(deptIds);
		} else if(scopeType == 3) {
			// 指定用户
			List<String> userIds = new ArrayList<>();
			for(String s : scopeIds.split(",")) {
				String trimmed = s.trim();
				if(!trimmed.isEmpty()) {
					userIds.add(trimmed);
				}
			}
			return userIds;
		}
		return new ArrayList<>();
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
	 * 获取用户未阅读公告（排除已撤回）
	 * @param tsysUser
	 * @param state 阅读状态  0未阅读 1 阅读  -1全部
	 * @return
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
			// 排除已撤回的公告
			noticeExample.createCriteria().andIdIn(ids).andStatusNotEqualTo(1);
			notices=sysNoticeMapper.selectByExample(noticeExample);
		}
		return notices;
	}


	/**
	 * 根据公告id把当前用户的公告置为已查看（幂等，已撤回不可读）
	 * @param noticeid
	 */
	public void editUserState(String noticeid) {
		// 检查公告是否存在且未撤回
		SysNotice notice = sysNoticeMapper.selectByPrimaryKey(noticeid);
		if(notice == null || notice.getStatus() != null && notice.getStatus() == 1) {
			return;
		}
		//SysNoticeUser
		SysNoticeUserExample sysNoticeUserExample=new SysNoticeUserExample();
		sysNoticeUserExample.createCriteria().andNoticeIdEqualTo(noticeid).andUserIdEqualTo(SaTokenUtil.getUserId());
		List<SysNoticeUser> noticeUsers= sysNoticeUserMapper.selectByExample(sysNoticeUserExample);
		for (SysNoticeUser sysNoticeUser : noticeUsers) {
			// 幂等：已读则跳过
			if(sysNoticeUser.getState() != null && sysNoticeUser.getState() == 1) {
				continue;
			}
			sysNoticeUser.setState(1);
			sysNoticeUser.setReadTime(new Date());
			sysNoticeUserMapper.updateByPrimaryKeySelective(sysNoticeUser);
		}
	}

	/**
	 * 撤回公告
	 * @param noticeId 公告ID
	 * @return 影响行数
	 */
	public int withdraw(String noticeId) {
		SysNotice notice = sysNoticeMapper.selectByPrimaryKey(noticeId);
		if(notice == null) {
			return 0;
		}
		// 只有正常状态的公告可以撤回
		if(notice.getStatus() != null && notice.getStatus() == 1) {
			return 0;
		}
		SysNotice update = new SysNotice();
		update.setId(noticeId);
		update.setStatus(1);
		return sysNoticeMapper.updateByPrimaryKeySelective(update);
	}

	/**
	 * 统计用户未读公告数量
	 * @param userId 用户ID
	 * @return 未读数量
	 */
	public int countUnread(String userId) {
		return noticeDao.countUnreadByUserId(userId);
	}

	/**
	 * 用户公告分页列表
	 * @param userId 用户ID
	 * @param tablepar 分页参数
	 * @param title 标题搜索
	 * @return 分页结果
	 */
	public PageInfo<SysNotice> getUserNoticePage(String userId, Tablepar tablepar, String title) {
		PageHelper.startPage(tablepar.getPage(), tablepar.getLimit());
		List<SysNotice> list = noticeDao.selectUserNoticePage(userId, title);
		return new PageInfo<>(list);
	}

	/**
	 * 用户公告详情（查看时自动标记已读）
	 * @param noticeId 公告ID
	 * @param userId 用户ID
	 * @return 公告详情，不存在或已撤回返回null
	 */
	@Transactional
	public SysNotice getUserNoticeDetail(String noticeId, String userId) {
		SysNotice notice = noticeDao.selectUserNoticeDetail(noticeId, userId);
		if(notice != null) {
			// 查看时自动标记已读
			markRead(noticeId, userId);
			// 刷新state为已读
			notice.setState(1);
		}
		return notice;
	}

	/**
	 * 标记已读（幂等）
	 * @param noticeId 公告ID
	 * @param userId 用户ID
	 */
	public void markRead(String noticeId, String userId) {
		SysNoticeUserExample example = new SysNoticeUserExample();
		example.createCriteria().andNoticeIdEqualTo(noticeId).andUserIdEqualTo(userId);
		List<SysNoticeUser> list = sysNoticeUserMapper.selectByExample(example);
		for(SysNoticeUser nu : list) {
			// 幂等：已读则跳过
			if(nu.getState() != null && nu.getState() == 1) {
				continue;
			}
			nu.setState(1);
			nu.setReadTime(new Date());
			sysNoticeUserMapper.updateByPrimaryKeySelective(nu);
		}
	}

	/**
	 * 获取公告已读统计
	 * @param noticeId 公告ID
	 * @return 统计VO
	 */
	public NoticeReadStatVO getReadStats(String noticeId) {
		return noticeDao.selectReadStatByNoticeId(noticeId);
	}

	/**
	 * 获取最新8条公告（排除已撤回）
	 * @return
	 */
	public List<SysNotice>  getNEW(){
        SysNoticeExample testExample=new SysNoticeExample();
        SysNoticeExample.Criteria criteria = testExample.createCriteria();
        criteria.andStatusNotEqualTo(1);
        testExample.setOrderByClause("id DESC");
        PageHelper.startPage(1, 8);
        List<SysNotice> list= sysNoticeMapper.selectByExample(testExample);
        return  list;
	 }


}
