package com.pousheng.middle.order.service;

import com.pousheng.middle.order.model.RefundAmount;
import io.terminus.common.model.Response;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/22
 * pousheng-middle
 */
public interface RefundAmountWriteService {
    /**
     * 新增 shipmentAmount
     * @param shipmentAmount
     * @return
     */
    Response<Long> create(RefundAmount refundAmount);

}
