package com.fc.v2.model.custom;

/**
 * 公告已读统计 VO
 * @author fuce
 */
public class NoticeReadStatVO {

    /** 总接收人数 **/
    private int totalCount;

    /** 已读人数 **/
    private int readCount;

    /** 未读人数 **/
    private int unreadCount;

    public NoticeReadStatVO() {
        super();
    }

    public NoticeReadStatVO(int totalCount, int readCount, int unreadCount) {
        this.totalCount = totalCount;
        this.readCount = readCount;
        this.unreadCount = unreadCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getReadCount() {
        return readCount;
    }

    public void setReadCount(int readCount) {
        this.readCount = readCount;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}
