package com.hhly.paycore.controller;

import com.alibaba.fastjson.JSONObject;
import com.hhly.paycore.service.TransUserService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.channel.bo.ChannelRechargeBO;
import com.hhly.skeleton.task.order.vo.OrderChannelVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.transform.Result;
import java.util.List;

@RestController
@RequestMapping("/channel/trans")
public class UserChannelTransController {

    @Autowired
    private TransUserService transUserService;

    @RequestMapping(value = "/recharge/list", method = RequestMethod.POST)
    public Object getChannelRechargeList(@RequestBody OrderChannelVO vo) {
        List<ChannelRechargeBO> channelRechargeList = transUserService.findChannelTransRechargeList(vo);
        return ResultBO.ok(channelRechargeList);
    }

}
