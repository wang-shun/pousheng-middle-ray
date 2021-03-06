/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.events.trade;

import lombok.Getter;

import java.io.Serializable;

/**
 * 换货发货事件
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-05-23
 */
public class RefundShipmentEvent implements Serializable {

    private static final long serialVersionUID = 2969280492730054880L;

    @Getter
    private final Long shipmentId;

    public RefundShipmentEvent(Long shipmentId) {
        this.shipmentId = shipmentId;
    }
}
