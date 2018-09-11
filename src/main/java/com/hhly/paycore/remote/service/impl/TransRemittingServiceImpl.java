package com.hhly.paycore.remote.service.impl;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.hhly.paycore.dao.TransRemittingDaoMapper;
import com.hhly.paycore.remote.service.ITransRemittingService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.cms.transmgr.bo.TransRemittingBO;

/**
 * @author TonyOne
 * @version 1.0
 * @desc
 * @date 2018/7/4 18:13
 * @company StayReal LTD
 */
@Component
@Service("iTransRemittingService")
public class TransRemittingServiceImpl implements ITransRemittingService{

    private static final Logger logger = Logger.getLogger(TransRemittingServiceImpl.class);

    @Autowired
    private TransRemittingDaoMapper transRemittingDaoMapper;

    @Override
    public ResultBO<?> insert(TransRemittingBO vo) {
        logger.info("插入汇款记录开始："+vo);
        int ret = transRemittingDaoMapper.insert(vo);
        logger.info("插入汇款记录成功："+vo);
        return ResultBO.ok(ret);
    }
}
