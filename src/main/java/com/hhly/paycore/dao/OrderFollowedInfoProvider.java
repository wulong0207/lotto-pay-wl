package com.hhly.paycore.dao;

public class OrderFollowedInfoProvider {
	
	public String selectOrderFollow(Integer orderIssueId){
		String sql = "select id id, user_id userId, nick_name nickName, order_issue_id orderIssueId, order_code orderCode, commission_amount commissionAmount, data_status dataStatus,"
				+ "create_time createTime, modify_time modifyTime, modify_by modifyBy, update_time updateTime FROM order_followed_info WHERE order_issue_id = "+orderIssueId;
		return sql;
	}
}
