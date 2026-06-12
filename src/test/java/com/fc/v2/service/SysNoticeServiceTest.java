package com.fc.v2.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fc.v2.mapper.auto.SysNoticeMapper;
import com.fc.v2.mapper.auto.SysNoticeUserMapper;
import com.fc.v2.mapper.custom.NoticeDao;
import com.fc.v2.model.auto.SysNotice;
import com.fc.v2.model.auto.SysNoticeUser;
import com.fc.v2.model.auto.SysNoticeUserExample;
import com.fc.v2.model.auto.TsysUser;
import com.fc.v2.model.auto.TsysUserExample;
import com.fc.v2.satoken.SaTokenUtil;
import com.fc.v2.util.SnowflakeIdWorker;

@ExtendWith(MockitoExtension.class)
public class SysNoticeServiceTest {

	@Mock
	private SysNoticeMapper sysNoticeMapper;
	@Mock
	private SysUserService sysUserService;
	@Mock
	private SysNoticeUserService sysNoticeUserService;
	@Mock
	private SysNoticeUserMapper sysNoticeUserMapper;
	@Mock
	private NoticeDao noticeDao;

	@InjectMocks
	private SysNoticeService sysNoticeService;

	// ======================== insertSelective tests ========================

	@Test
	public void testInsertSelective_scopeAll() {
		try (MockedStatic<SaTokenUtil> saUtil = mockStatic(SaTokenUtil.class);
			 MockedStatic<SnowflakeIdWorker> snowflake = mockStatic(SnowflakeIdWorker.class)) {

			saUtil.when(SaTokenUtil::getUserId).thenReturn("admin1");
			saUtil.when(SaTokenUtil::getLoginName).thenReturn("admin");
			snowflake.when(SnowflakeIdWorker::getUUID).thenReturn("notice-id-1");

			TsysUser u1 = new TsysUser(); u1.setId("user1");
			TsysUser u2 = new TsysUser(); u2.setId("user2");
			when(sysUserService.selectByExample(any(TsysUserExample.class))).thenReturn(Arrays.asList(u1, u2));
			when(sysNoticeMapper.insertSelective(any())).thenReturn(1);
			when(sysNoticeUserService.insertSelective(any())).thenReturn(1);

			SysNotice notice = new SysNotice();
			notice.setTitle("Test");
			notice.setScope(0);
			int result = sysNoticeService.insertSelective(notice);

			assertEquals(1, result);
			assertEquals("notice-id-1", notice.getId());
			assertEquals(1, notice.getStatus().intValue());
			verify(sysNoticeUserService, times(2)).insertSelective(any(SysNoticeUser.class));
		}
	}

	@Test
	public void testInsertSelective_scopeByRole() {
		try (MockedStatic<SaTokenUtil> saUtil = mockStatic(SaTokenUtil.class);
			 MockedStatic<SnowflakeIdWorker> snowflake = mockStatic(SnowflakeIdWorker.class)) {

			saUtil.when(SaTokenUtil::getUserId).thenReturn("admin1");
			saUtil.when(SaTokenUtil::getLoginName).thenReturn("admin");
			snowflake.when(SnowflakeIdWorker::getUUID).thenReturn("notice-id-2");

			when(noticeDao.selectUserIdsByRoleIds(anyList())).thenReturn(Arrays.asList("user1", "user3"));
			when(sysNoticeMapper.insertSelective(any())).thenReturn(1);
			when(sysNoticeUserService.insertSelective(any())).thenReturn(1);

			SysNotice notice = new SysNotice();
			notice.setTitle("Role Notice");
			notice.setScope(1);
			notice.setTargetIds("role1,role2");
			sysNoticeService.insertSelective(notice);

			verify(noticeDao).selectUserIdsByRoleIds(anyList());
			verify(sysNoticeUserService, times(2)).insertSelective(any(SysNoticeUser.class));
		}
	}

	@Test
	public void testInsertSelective_scopeByDept() {
		try (MockedStatic<SaTokenUtil> saUtil = mockStatic(SaTokenUtil.class);
			 MockedStatic<SnowflakeIdWorker> snowflake = mockStatic(SnowflakeIdWorker.class)) {

			saUtil.when(SaTokenUtil::getUserId).thenReturn("admin1");
			saUtil.when(SaTokenUtil::getLoginName).thenReturn("admin");
			snowflake.when(SnowflakeIdWorker::getUUID).thenReturn("notice-id-3");

			when(noticeDao.selectUserIdsByDeptIds(anyList())).thenReturn(Arrays.asList("user4"));
			when(sysNoticeMapper.insertSelective(any())).thenReturn(1);
			when(sysNoticeUserService.insertSelective(any())).thenReturn(1);

			SysNotice notice = new SysNotice();
			notice.setTitle("Dept Notice");
			notice.setScope(2);
			notice.setTargetIds("2,3");
			sysNoticeService.insertSelective(notice);

			ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
			verify(noticeDao).selectUserIdsByDeptIds(captor.capture());
			List<Integer> deptIds = captor.getValue();
			assertEquals(Arrays.asList(2, 3), deptIds);
			verify(sysNoticeUserService, times(1)).insertSelective(any(SysNoticeUser.class));
		}
	}

	@Test
	public void testInsertSelective_scopeByUser() {
		try (MockedStatic<SaTokenUtil> saUtil = mockStatic(SaTokenUtil.class);
			 MockedStatic<SnowflakeIdWorker> snowflake = mockStatic(SnowflakeIdWorker.class)) {

			saUtil.when(SaTokenUtil::getUserId).thenReturn("admin1");
			saUtil.when(SaTokenUtil::getLoginName).thenReturn("admin");
			snowflake.when(SnowflakeIdWorker::getUUID).thenReturn("notice-id-4");

			when(sysNoticeMapper.insertSelective(any())).thenReturn(1);
			when(sysNoticeUserService.insertSelective(any())).thenReturn(1);

			SysNotice notice = new SysNotice();
			notice.setTitle("User Notice");
			notice.setScope(3);
			notice.setTargetIds("user5,user6,user7");
			sysNoticeService.insertSelective(notice);

			verify(sysNoticeUserService, times(3)).insertSelective(any(SysNoticeUser.class));
		}
	}

	// ======================== recallNotice tests ========================

	@Test
	public void testRecallNotice_success() {
		SysNotice notice = new SysNotice();
		notice.setId("n1");
		notice.setStatus(1);
		when(sysNoticeMapper.selectByPrimaryKey("n1")).thenReturn(notice);
		when(sysNoticeMapper.updateByPrimaryKeySelective(any())).thenReturn(1);

		int result = sysNoticeService.recallNotice("n1");
		assertEquals(1, result);

		ArgumentCaptor<SysNotice> captor = ArgumentCaptor.forClass(SysNotice.class);
		verify(sysNoticeMapper).updateByPrimaryKeySelective(captor.capture());
		assertEquals(2, captor.getValue().getStatus().intValue());
	}

	@Test
	public void testRecallNotice_alreadyRecalled() {
		SysNotice notice = new SysNotice();
		notice.setId("n2");
		notice.setStatus(2);
		when(sysNoticeMapper.selectByPrimaryKey("n2")).thenReturn(notice);

		int result = sysNoticeService.recallNotice("n2");
		assertEquals(0, result);
		verify(sysNoticeMapper, never()).updateByPrimaryKeySelective(any());
	}

	@Test
	public void testRecallNotice_notFound() {
		when(sysNoticeMapper.selectByPrimaryKey("n3")).thenReturn(null);

		int result = sysNoticeService.recallNotice("n3");
		assertEquals(0, result);
		verify(sysNoticeMapper, never()).updateByPrimaryKeySelective(any());
	}

	// ======================== editUserState tests ========================

	@Test
	public void testEditUserState_firstRead() {
		try (MockedStatic<SaTokenUtil> saUtil = mockStatic(SaTokenUtil.class)) {
			saUtil.when(SaTokenUtil::getUserId).thenReturn("user1");

			SysNotice notice = new SysNotice();
			notice.setStatus(1);
			when(sysNoticeMapper.selectByPrimaryKey("n1")).thenReturn(notice);

			SysNoticeUser nu = new SysNoticeUser("nu1", "n1", "user1", 0, null);
			when(sysNoticeUserMapper.selectByExample(any(SysNoticeUserExample.class)))
				.thenReturn(Collections.singletonList(nu));
			when(sysNoticeUserMapper.updateByPrimaryKey(any())).thenReturn(1);

			sysNoticeService.editUserState("n1");

			assertEquals(1, nu.getState().intValue());
			assertNotNull(nu.getReadTime());
			verify(sysNoticeUserMapper).updateByPrimaryKey(nu);
		}
	}

	@Test
	public void testEditUserState_idempotent() {
		try (MockedStatic<SaTokenUtil> saUtil = mockStatic(SaTokenUtil.class)) {
			saUtil.when(SaTokenUtil::getUserId).thenReturn("user1");

			SysNotice notice = new SysNotice();
			notice.setStatus(1);
			when(sysNoticeMapper.selectByPrimaryKey("n1")).thenReturn(notice);

			Date originalReadTime = new Date(1000000L);
			SysNoticeUser nu = new SysNoticeUser("nu1", "n1", "user1", 1, originalReadTime);
			when(sysNoticeUserMapper.selectByExample(any(SysNoticeUserExample.class)))
				.thenReturn(Collections.singletonList(nu));

			sysNoticeService.editUserState("n1");

			// Should NOT call update -- already read
			verify(sysNoticeUserMapper, never()).updateByPrimaryKey(any());
			assertEquals(originalReadTime, nu.getReadTime());
		}
	}

	@Test
	public void testEditUserState_recalledNotice() {
		SysNotice notice = new SysNotice();
		notice.setStatus(2);
		when(sysNoticeMapper.selectByPrimaryKey("n1")).thenReturn(notice);

		sysNoticeService.editUserState("n1");

		// Should NOT even query notice_user
		verify(sysNoticeUserMapper, never()).selectByExample(any());
		verify(sysNoticeUserMapper, never()).updateByPrimaryKey(any());
	}

	// ======================== getUnreadCount test ========================

	@Test
	public void testGetUnreadCount() {
		when(noticeDao.countUnreadByUserId("user1")).thenReturn(5);

		int count = sysNoticeService.getUnreadCount("user1");
		assertEquals(5, count);
		verify(noticeDao).countUnreadByUserId("user1");
	}
}
