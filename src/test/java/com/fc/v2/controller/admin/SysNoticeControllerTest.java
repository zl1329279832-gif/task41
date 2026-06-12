package com.fc.v2.controller.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ModelMap;

import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.model.auto.SysNotice;
import com.fc.v2.model.auto.SysNoticeUser;
import com.fc.v2.model.auto.SysNoticeUserExample;
import com.fc.v2.model.custom.Tablepar;
import com.fc.v2.satoken.SaTokenUtil;
import com.fc.v2.service.SysNoticeService;
import com.fc.v2.service.SysNoticeUserService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

@ExtendWith(MockitoExtension.class)
public class SysNoticeControllerTest {

	@Mock
	private SysNoticeService sysNoticeService;
	@Mock
	private SysNoticeUserService sysNoticeUserService;

	@InjectMocks
	private SysNoticeController controller;

	// ======================== recall tests ========================

	@Test
	public void testRecall_success() {
		when(sysNoticeService.recallNotice("n1")).thenReturn(1);

		AjaxResult result = controller.recall("n1");

		assertEquals(200, result.get("code"));
		assertEquals("撤回成功", result.get("msg"));
	}

	@Test
	public void testRecall_failNotFound() {
		when(sysNoticeService.recallNotice("n2")).thenReturn(0);

		AjaxResult result = controller.recall("n2");

		assertEquals(500, result.get("code"));
		assertTrue(result.get("msg").toString().contains("撤回失败"));
	}

	// ======================== unreadCount test ========================

	@Test
	public void testUnreadCount() {
		try (MockedStatic<SaTokenUtil> saUtil = mockStatic(SaTokenUtil.class)) {
			saUtil.when(SaTokenUtil::getUserId).thenReturn("user1");
			when(sysNoticeService.getUnreadCount("user1")).thenReturn(3);

			AjaxResult result = controller.unreadCount();

			assertEquals(200, result.get("code"));
			assertEquals(3, result.get("data"));
		}
	}

	// ======================== viewinfo tests ========================

	@Test
	public void testViewinfo_normalNotice() {
		SysNotice notice = new SysNotice();
		notice.setId("n1");
		notice.setStatus(1);
		notice.setTitle("Test");
		when(sysNoticeService.selectByPrimaryKey("n1")).thenReturn(notice);

		ModelMap mmap = new ModelMap();
		String view = controller.viewinfo("n1", mmap);

		assertEquals("admin/sysNotice/view", view);
		assertEquals(notice, mmap.get("notice"));
		assertNull(mmap.get("recalled"));
		verify(sysNoticeService).editUserState("n1");
	}

	@Test
	public void testViewinfo_recalledNotice() {
		SysNotice notice = new SysNotice();
		notice.setId("n2");
		notice.setStatus(2);
		when(sysNoticeService.selectByPrimaryKey("n2")).thenReturn(notice);

		ModelMap mmap = new ModelMap();
		String view = controller.viewinfo("n2", mmap);

		assertEquals("admin/sysNotice/view", view);
		assertNull(mmap.get("notice"));
		assertEquals(true, mmap.get("recalled"));
		verify(sysNoticeService, never()).editUserState(anyString());
	}

	// ======================== add with scope test ========================

	@Test
	public void testAdd_withScope() {
		when(sysNoticeService.insertSelective(any(SysNotice.class))).thenReturn(1);

		SysNotice notice = new SysNotice();
		notice.setTitle("Scoped Notice");
		notice.setScope(1);
		notice.setTargetIds("role1,role2");

		AjaxResult result = controller.add(notice);

		assertEquals(200, result.get("code"));
		verify(sysNoticeService).insertSelective(notice);
	}
}
