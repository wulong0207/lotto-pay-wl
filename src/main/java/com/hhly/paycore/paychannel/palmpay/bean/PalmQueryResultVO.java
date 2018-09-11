package com.hhly.paycore.paychannel.palmpay.bean;

/**
 * @desc 掌宜付支付结果查询结果
 * @author xiongJinGang
 * @date 2017年9月16日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class PalmQueryResultVO {
	private String code;// 0 交易成功1 交易处理中2 交易未完成 （超过60秒的交易）3 无此订单号其他为错误代码
	private String message;// 结果说明

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
