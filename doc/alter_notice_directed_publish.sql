-- ============================================================
-- 站内公告定向发布与已读回执 - 数据库变更脚本
-- 执行前请备份相关表数据
-- ============================================================

-- t_sys_notice 新增 3 列
ALTER TABLE t_sys_notice
  ADD COLUMN status     INT    DEFAULT 0    COMMENT '0-正常 1-已撤回',
  ADD COLUMN scope_type INT    DEFAULT 0    COMMENT '0-全部用户 1-指定角色 2-指定部门 3-指定用户',
  ADD COLUMN scope_ids  TEXT                COMMENT 'scope_type!=0时存储的目标ID列表(逗号分隔)';

-- t_sys_notice_user 新增 1 列
ALTER TABLE t_sys_notice_user
  ADD COLUMN read_time  DATETIME DEFAULT NULL COMMENT '已读时间';
