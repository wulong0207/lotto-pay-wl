package com.hhly.paycore.dao;

import com.hhly.skeleton.pay.bo.OrderGroupBO;

public interface OrderGroupMapper {

	OrderGroupBO getOrderGroupByOrderCode(String orderCode);
}