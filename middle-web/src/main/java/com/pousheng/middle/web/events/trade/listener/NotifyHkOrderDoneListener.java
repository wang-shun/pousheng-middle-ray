package com.pousheng.middle.web.events.trade.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.model.PoushengSettlementPos;
import com.pousheng.middle.order.service.PoushengSettlementPosReadService;
import com.pousheng.middle.order.service.PoushengSettlementPosWriteService;
import com.pousheng.middle.web.events.trade.NotifyHkOrderDoneEvent;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentLogic;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 通知恒康电商平台已经收货的事件
 * Created by tony on 2017/8/25.
 * pousheng-middle
 */
@Slf4j
@Component
public class NotifyHkOrderDoneListener {
    @Autowired
    private EventBus eventBus;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private SyncShipmentLogic syncShipmentLogic;
    @Autowired
    private PoushengSettlementPosReadService poushengSettlementPosReadService;
    @Autowired
    private PoushengSettlementPosWriteService poushengSettlementPosWriteService;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void notifyHkOrderConfirmed(NotifyHkOrderDoneEvent event) {
        Long shopOrderId = event.getShopOrderId();
        List<OrderShipment> orderShipments =  shipmentReadLogic.findByOrderIdAndType(shopOrderId);
        //获取已发货的发货单
        List<OrderShipment> orderShipmentsFilter = orderShipments.stream().filter(Objects::nonNull)
                .filter(orderShipment -> Objects.equals(orderShipment.getStatus(),MiddleShipmentsStatus.SHIPPED.getValue()))
                .collect(Collectors.toList());
        for (OrderShipment orderShipment:orderShipmentsFilter){
            //通知恒康已经发货
            Long shipmentId = orderShipment.getShipmentId();
            Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
            Response<Boolean> response= syncShipmentLogic.syncShipmentDoneToHk(shipment,2, MiddleOrderEvent.AUTO_HK_CONFIRME_FAILED.toOrderOperation());
            if (!response.isSuccess()){
                log.error("notify hk order confirm failed,shipment id is ({}),caused by {}",shipment.getId(),response.getError());
            }
            Response<PoushengSettlementPos> r = poushengSettlementPosReadService.findByShipmentId(shipmentId);
            if (!r.isSuccess()){
                log.error("failed find settlement pos, shipmentId={}, cause:{}",shipmentId, r.getError());
            }
            if (Objects.nonNull(r.getResult())){
                PoushengSettlementPos pos = new PoushengSettlementPos();
                pos.setId(r.getResult().getId());
                pos.setPosDoneAt(new Date());
                Response<Boolean> rr = poushengSettlementPosWriteService.update(pos);
                if (!rr.isSuccess()){
                    log.error("update pos done time failed,pousheng settlement pos id is {},caused by {}",r.getResult().getId(),rr.getError());
                }
            }
        }
    }
}
