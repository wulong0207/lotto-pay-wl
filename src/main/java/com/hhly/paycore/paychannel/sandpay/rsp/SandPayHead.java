package com.hhly.paycore.paychannel.sandpay.rsp;

/**
 * @desc 订单查询返回头部
 * @author xiongJinGang
 * @date 2018年6月27日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class SandPayHead {
	private String respTime;
	private String respMsg;
	private String respCode;

	public String getRespTime() {
		return respTime;
	}

	public void setRespTime(String respTime) {
		this.respTime = respTime;
	}

	public String getRespMsg() {
		return respMsg;
	}

	public void setRespMsg(String respMsg) {
		this.respMsg = respMsg;
	}

	public String getRespCode() {
		return respCode;
	}

	public void setRespCode(String respCode) {
		this.respCode = respCode;
	}

}
