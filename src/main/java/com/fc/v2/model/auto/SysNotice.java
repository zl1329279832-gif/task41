package com.fc.v2.model.auto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fc.v2.util.DateUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 公告 SysNotice 
 * @author fuce_自动生成
 * @email 115889198@qq.com
 * @date 2019-09-08 01:38:44
 */
public class SysNotice implements Serializable {

	private static final long serialVersionUID = 1L;
	
		
	/** 主键 **/
	private String id;
		
	/** 标题 **/
	private String title;
		
	/** 内容 **/
	private String content;
		
	/** 类型 **/
	private Integer type;
		
	/** 创建人id **/
	private String createId;
		
	/** 创建人name **/
	private String createUsername;
		
	/** 发信时间 **/
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
	private Date createTime;

	/** 状态: 0=draft, 1=published, 2=recalled **/
	private Integer status;

	/** 发送范围: 0=all, 1=by_role, 2=by_dept, 3=by_user **/
	private Integer scope;

	/** 目标IDs: 逗号分隔 **/
	private String targetIds;


	public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
	 
			
	public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
	 
			
	public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
	 
			
	public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }
	 
			
	public String getCreateId() {
        return createId;
    }

    public void setCreateId(String createId) {
        this.createId = createId;
    }
	 
			
	public String getCreateUsername() {
        return createUsername;
    }

    public void setCreateUsername(String createUsername) {
        this.createUsername = createUsername;
    }
	 
			
	public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }


	public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

	public Integer getScope() {
        return scope;
    }

    public void setScope(Integer scope) {
        this.scope = scope;
    }

	public String getTargetIds() {
        return targetIds;
    }

    public void setTargetIds(String targetIds) {
        this.targetIds = targetIds;
    }


	public SysNotice() {
        super();
    }
    
																																					
	public SysNotice(String id,String title,String content,Integer type,String createId,String createUsername,Date createTime,Integer status,Integer scope,String targetIds) {

		this.id = id;
		this.title = title;
		this.content = content;
		this.type = type;
		this.createId = createId;
		this.createUsername = createUsername;
		this.createTime = createTime;
		this.status = status;
		this.scope = scope;
		this.targetIds = targetIds;
	}

    /**
     * 格式化时间
     * @return YYYY_MM_DD
     */
	public String getdate(){
       return  DateUtils.dateTime(this.createTime);
    }
	
}