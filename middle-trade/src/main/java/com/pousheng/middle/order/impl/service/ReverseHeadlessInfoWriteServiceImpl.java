package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.impl.dao.ReverseHeadlessInfoDao;
import com.pousheng.middle.order.model.ReverseHeadlessInfo;
import com.pousheng.middle.order.service.ReverseHeadlessInfoWriteService;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Code generated by terminus code gen
 * Desc: 写服务实现类
 * Date: 2019-06-03
 */
@Slf4j
@Service
public class ReverseHeadlessInfoWriteServiceImpl implements ReverseHeadlessInfoWriteService {

    private final ReverseHeadlessInfoDao reverseHeadlessInfoDao;

    @Autowired
    public ReverseHeadlessInfoWriteServiceImpl(ReverseHeadlessInfoDao reverseHeadlessInfoDao) {
        this.reverseHeadlessInfoDao = reverseHeadlessInfoDao;
    }

    @Override
    public Response<Long> createReverseHeadlessInfo(ReverseHeadlessInfo reverseHeadlessInfo) {
        try {
            reverseHeadlessInfoDao.create(reverseHeadlessInfo);
            return Response.ok(reverseHeadlessInfo.getId());
        } catch (Exception e) {
            log.error("create reverseHeadlessInfo failed, reverseHeadlessInfo:{}, cause:{}", reverseHeadlessInfo, Throwables.getStackTraceAsString(e));
            return Response.fail("reverseHeadlessInfo.create.fail");
        }
    }

    @Override
    public Response<Long> batchCreateOrUpdate(List<ReverseHeadlessInfo> reverseHeadlessInfoList) {
        try {
            reverseHeadlessInfoDao.batchCreate(reverseHeadlessInfoList);
            return Response.ok();
        } catch (Exception e) {
            log.error("batch create reverseHeadlessInfo failed, reverseHeadlessInfo:{}, cause:{}", JsonMapper.nonEmptyMapper().toJson(reverseHeadlessInfoList), Throwables.getStackTraceAsString(e));
            return Response.fail("reverseHeadlessInfo.batch.create.fail");
        }
    }

    @Override
    public Response<Boolean> updateReverseHeadlessInfo(ReverseHeadlessInfo reverseHeadlessInfo) {
        try {
            return Response.ok(reverseHeadlessInfoDao.update(reverseHeadlessInfo));
        } catch (Exception e) {
            log.error("update reverseHeadlessInfo failed, reverseHeadlessInfo:{}, cause:{}", reverseHeadlessInfo, Throwables.getStackTraceAsString(e));
            return Response.fail("reverseHeadlessInfo.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteReverseHeadlessInfoById(Long reverseHeadlessInfoId) {
        try {
            return Response.ok(reverseHeadlessInfoDao.delete(reverseHeadlessInfoId));
        } catch (Exception e) {
            log.error("delete reverseHeadlessInfo failed, reverseHeadlessInfoId:{}, cause:{}", reverseHeadlessInfoId, Throwables.getStackTraceAsString(e));
            return Response.fail("reverseHeadlessInfo.delete.fail");
        }
    }
}
