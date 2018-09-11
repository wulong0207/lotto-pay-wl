package com.hhly.paycore.paychannel.sandpay.rsp;

public class SandPayRsp {
	private SandPayHead head;
	private SandPayBody body;

	public SandPayHead getHead() {
		return head;
	}

	public void setHead(SandPayHead head) {
		this.head = head;
	}

	public SandPayBody getBody() {
		return body;
	}

	public void setBody(SandPayBody body) {
		this.body = body;
	}

}
