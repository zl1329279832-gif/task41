package com.fc.v2.mapper.custom;

import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 公告自定义查询 NoticeDao
 * 用于按角色/部门解析用户ID、统计未读数
 */
public interface NoticeDao {

    /**
     * 根据角色ID列表查询关联的用户ID
     */
    List<String> selectUserIdsByRoleIds(@Param("roleIds") List<String> roleIds);

    /**
     * 根据部门ID列表查询关联的用户ID
     */
    List<String> selectUserIdsByDeptIds(@Param("deptIds") List<Integer> deptIds);

    /**
     * 统计用户未读公告数量（排除已撤回的公告）
     */
    int countUnreadByUserId(@Param("userId") String userId);
}
