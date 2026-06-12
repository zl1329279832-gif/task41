-- ============================================================
-- Notice Targeted Publishing & Read Receipt Migration
-- ============================================================

-- 1. Add status/scope/target_ids to t_sys_notice
ALTER TABLE t_sys_notice
  ADD COLUMN `status`     int(2)        NOT NULL DEFAULT 1 COMMENT '0=draft,1=published,2=recalled',
  ADD COLUMN `scope`      int(2)        NOT NULL DEFAULT 0 COMMENT '0=all,1=by_role,2=by_dept,3=by_user',
  ADD COLUMN `target_ids` varchar(2000) DEFAULT NULL COMMENT 'comma-separated target IDs for the scope';

-- 2. Add read_time to t_sys_notice_user
ALTER TABLE t_sys_notice_user
  ADD COLUMN `read_time`  datetime      DEFAULT NULL COMMENT 'first read timestamp';

-- 3. Indexes for common query patterns
ALTER TABLE t_sys_notice_user ADD INDEX idx_user_state (user_id, state);
ALTER TABLE t_sys_notice ADD INDEX idx_status (status);
