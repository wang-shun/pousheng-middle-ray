package com.pousheng.middle.web.biz.impl;

import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.PoushengMiddleCompensateBizService;
import com.pousheng.middle.web.biz.annotation.PoushengMiddleCompensateAnnotation;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/5/28
 * pousheng-middle
 */
@PoushengMiddleCompensateAnnotation(bizType = PoushengCompensateBizType.NOTIFY_HK)
@Service
@Slf4j
public class HkNotifyService  implements PoushengMiddleCompensateBizService{
    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) throws BizException {
        // TODO: 2018/5/28 将来需要删除
        log.info("============================》");
    }
}
