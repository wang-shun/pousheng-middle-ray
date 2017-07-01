/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.events.trade.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.web.events.trade.OrderShipmentEvent;
import com.pousheng.middle.web.events.trade.RefundShipmentEvent;
import com.pousheng.middle.web.order.component.OrderWriteLogic;
import com.pousheng.middle.web.order.component.RefundWriteLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import io.swagger.models.auth.In;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.service.ShipmentReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 更新换货商品已处理数量
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-05-23
 */
@Slf4j
public class RefundShipmentListener {


    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private RefundWriteLogic refundWriteLogic;

    @Autowired
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void onRefundShipment(RefundShipmentEvent refundShipmentEvent) {
        Long shipmentId = refundShipmentEvent.getShipmentId();
        Shipment shipment  = shipmentReadLogic.findShipmentById(shipmentId);
        OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipmentId);
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);

        Map<String, Integer> skuCodeAndQuantityMap = shipmentItems.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(ShipmentItem::getSkuCode, ShipmentItem::getQuantity));

        refundWriteLogic.updateSkuHandleNumber(orderShipment.getAfterSaleOrderId(),skuCodeAndQuantityMap);

    }
}
