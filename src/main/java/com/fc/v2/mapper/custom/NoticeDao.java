package com.fc.v2.mapper.custom;

import com.fc.v2.model.auto.SysNotice;
import com.fc.v2.model.auto.SysNoticeUser;
import com.fc.v2.model.custom.NoticeReadStatVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 公告自定义Dao
 * @ClassName: NoticeDao
 * @author fuce
 */
public interface NoticeDao {

    /**
     * 根据角色ID列表查询关联的用户ID（快照）
     * @param roleIds 角色ID列表
     * @return 用户ID列表
     */
    List<String> selectUserIdsByRoleIds(@Param("roleIds") List<String> roleIds);

    /**
     * 根据部门ID列表查询关联的用户ID（快照）
     * @param deptIds 部门ID列表
     * @return 用户ID列表
     */
    List<String> selectUserIdsByDeptIds(@Param("deptIds") List<Integer> deptIds);

    /**
     * 统计用户未读公告数量（排除已撤回的公告）
     * @param userId 用户ID
     * @return 未读数量
     */
    int countUnreadByUserId(@Param("userId") String userId);

    /**
     * 批量插入 notice_user 记录
     * @param list SysNoticeUser列表
     * @return 插入行数
     */
    int batchInsertNoticeUser(@Param("list") List<SysNoticeUser> list);

    /**
     * 用户公告分页列表（JOIN 查询，含 state 和 read_time）
     * @param userId 用户ID
     * @param title 标题模糊搜索（可为null）
     * @return 公告列表
     */
    List<SysNotice> selectUserNoticePage(@Param("userId") String userId, @Param("title") String title);

    /**
     * 用户公告详情（单条 JOIN 查询，含 state 和 read_time）
     * @param noticeId 公告ID
     * @param userId 用户ID
     * @return 公告详情（不存在或已撤回返回null）
     */
    SysNotice selectUserNoticeDetail(@Param("noticeId") String noticeId, @Param("userId") String userId);

    /**
     * 查询公告的已读/未读统计
     * @param noticeId 公告ID
     * @return 统计VO
     */
    NoticeReadStatVO selectReadStatByNoticeId(@Param("noticeId") String noticeId);
}
