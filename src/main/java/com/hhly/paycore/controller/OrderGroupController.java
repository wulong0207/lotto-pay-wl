package com.hhly.paycore.controller;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.hhly.paycore.service.OrderGroupService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.vo.OrderGroupVO;

/**
 * @desc 代理输入输出控制层
 * @author xiongJinGang
 * @date 2018年3月2日
 * @company 益彩网络科技公司
 * @version 1.0
 */
@RestController
@RequestMapping("/orderGroup")
public class OrderGroupController {

	private static final Logger logger = LoggerFactory.getLogger(OrderGroupController.class);

	@Resource
	private OrderGroupService orderGroupService;

	/**  
	* 方法说明: 未满员，平台认购
	* @auth: xiongJinGang
	* @param orderGroupVO
	* @throws Exception
	* @time: 2018年5月4日 下午2:37:41
	* @return: ResultBO<?> 
	*/
	@RequestMapping(value = "/platformAdvance", method = RequestMethod.POST)
	public ResultBO<?> findBankList(@RequestBody OrderGroupVO orderGroupVO) {
		try {
			logger.info("未满员，平台保底认购参数：" + orderGroupVO.toString());
			// 如果订单号或者支付金额，或者用户ID为空，返回参数错误
			if (ObjectUtil.isBlank(orderGroupVO.getOrderCode()) || ObjectUtil.isBlank(orderGroupVO.getBuyAmount()) || ObjectUtil.isBlank(orderGroupVO.getUserId()) || ObjectUtil.isBlank(orderGroupVO.getBuyCode())) {
				return ResultBO.err(MessageCodeConstants.PARAM_IS_NULL_FIELD);
			}
			// 更新平台合买信息
			return orderGroupService.updateOrderGroupByPlatform(orderGroupVO);
		} catch (Exception e) {
			logger.error("订单：" + orderGroupVO.getOrderCode() + "未满员，平台进行保底异常", e);
			return ResultBO.err(MessageCodeConstants.SYS_ERROR_SYS);
		}
	}

}
