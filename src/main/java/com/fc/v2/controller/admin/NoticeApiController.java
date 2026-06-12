package com.fc.v2.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.model.auto.SysNotice;
import com.fc.v2.model.custom.Tablepar;
import com.fc.v2.satoken.SaTokenUtil;
import com.fc.v2.service.SysNoticeService;
import com.github.pagehelper.PageInfo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 公告用户端API（JSON接口）
 * @ClassName: NoticeApiController
 * @author fuce
 */
@Api(value = "公告用户API")
@RestController
@RequestMapping("/NoticeApiController")
public class NoticeApiController extends BaseController {

	@Autowired
	private SysNoticeService sysNoticeService;

	/**
	 * 当前用户的公告分页列表
	 * @param tablepar 分页参数
	 * @param searchText 标题搜索
	 * @return 分页结果
	 */
	@ApiOperation(value = "用户公告列表", notes = "当前用户的公告分页列表（排除已撤回）")
	@GetMapping("/list")
	public ResultTable list(Tablepar tablepar, String searchText) {
		String userId = SaTokenUtil.getUserId();
		PageInfo<SysNotice> page = sysNoticeService.getUserNoticePage(userId, tablepar, searchText);
		return pageTable(page.getList(), page.getTotal());
	}

	/**
	 * 当前用户未读公告数
	 * @return 未读数量
	 */
	@ApiOperation(value = "未读公告数", notes = "当前用户未读公告数量")
	@GetMapping("/unreadCount")
	public AjaxResult unreadCount() {
		String userId = SaTokenUtil.getUserId();
		int count = sysNoticeService.countUnread(userId);
		return retobject(200, count);
	}

	/**
	 * 查看公告详情（自动标记已读）
	 * @param id 公告ID
	 * @return 公告详情
	 */
	@ApiOperation(value = "公告详情", notes = "查看公告详情，自动标记已读")
	@GetMapping("/detail/{id}")
	public AjaxResult detail(@PathVariable("id") String id) {
		String userId = SaTokenUtil.getUserId();
		SysNotice notice = sysNoticeService.getUserNoticeDetail(id, userId);
		if (notice == null) {
			return error("公告不存在或已撤回");
		}
		return retobject(200, notice);
	}

	/**
	 * 显式提交已读回执（幂等）
	 * @param noticeId 公告ID
	 * @return 操作结果
	 */
	@ApiOperation(value = "标记已读", notes = "显式提交已读回执（幂等）")
	@PostMapping("/markRead")
	public AjaxResult markRead(String noticeId) {
		String userId = SaTokenUtil.getUserId();
		sysNoticeService.markRead(noticeId, userId);
		return success();
	}
}
