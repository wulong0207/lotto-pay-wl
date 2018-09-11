package com.hhly.paycore.dao;

import java.util.List;

import com.hhly.skeleton.pay.channel.bo.OperateMarketChannelBO;

/**
 * @desc 运营渠道
 * @author xiongJinGang
 * @date 2018年1月27日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface OperateMarketChannelMapper {

	List<OperateMarketChannelBO> getList();

}