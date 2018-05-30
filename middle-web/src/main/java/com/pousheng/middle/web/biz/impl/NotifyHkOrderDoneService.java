/**
 * Copyright (C), 2012-2018, XXX有限公司
 * FileName: NotifyHkOrderDoneService
 * Author:   xiehong
 * Date:     2018/5/30 下午4:54
 * Description: 通知恒康发货单时间任务
 * History:
 * <author>          <time>          <version>          <desc>
 * 作者姓名           修改时间           版本号              描述
 */
package com.pousheng.middle.web.biz.impl;

import com.google.common.collect.Maps;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.model.PoushengSettlementPos;
import com.pousheng.middle.order.service.PoushengSettlementPosReadService;
import com.pousheng.middle.order.service.PoushengSettlementPosWriteService;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.order.component.AutoCompensateLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import com.pousheng.middle.web.order.sync.erp.SyncErpShipmentLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentPosLogic;
import io.terminus.common.model.Response;
import io.terminus.msg.common.StringUtil;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 〈通知恒康发货单时间任务〉
 *
 * @author xiehong
 * @create 2018/5/30 下午4:54
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.NOTIFY_HK_ORDER_DOWN)
@Service
@Slf4j
public class NotifyHkOrderDoneService implements CompensateBizService {


    @Autowired
    private ShipmentReadLogic shipmentReadLogic;



    @Autowired
    private PoushengSettlementPosReadService poushengSettlementPosReadService;
    @Autowired
    private PoushengSettlementPosWriteService poushengSettlementPosWriteService;
    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @Autowired
    private SyncErpShipmentLogic syncErpShipmentLogic;
    @Autowired
    private SyncShipmentPosLogic syncShipmentPosLogic;
    @Autowired
    private AutoCompensateLogic autoCompensateLogic;


    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        if (null == poushengCompensateBiz) {
            log.warn("NotifyHkOrderDoneService.doProcess params is null");
            return;
        }
        String context = poushengCompensateBiz.getContext();
        if (StringUtil.isBlank(context)) {
            log.warn("NotifyHkOrderDoneService.doProcess context is null");
            return;
        }

        Long shopOrderId = Long.valueOf(context);
        List<OrderShipment> orderShipments = shipmentReadLogic.findByOrderIdAndType(shopOrderId);
        //获取已发货的发货单
        List<OrderShipment> orderShipmentsFilter = orderShipments.stream().filter(Objects::nonNull)
                .filter(orderShipment -> Objects.equals(orderShipment.getStatus(), MiddleShipmentsStatus.SHIPPED.getValue()))
                .collect(Collectors.toList());
        for (OrderShipment orderShipment : orderShipmentsFilter) {
            Long shipmentId = orderShipment.getShipmentId();
            Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
            //如果是店发，通知给hk（针对mpos订单）
            if (Objects.equals(shipmentExtra.getShipmentWay(), TradeConstants.MPOS_SHOP_DELIVER)) {
                Response<Boolean> res = shipmentWiteLogic.updateStatus(shipment, MiddleOrderEvent.HK_CONFIRMD_SUCCESS.toOrderOperation());
                if (!res.isSuccess()) {
                    log.error("shipment(id:{}) confirm failed,cause:{}", shipment.getId(), res.getError());
                }
                Response<Boolean> response = syncShipmentPosLogic.syncShipmentDoneToHk(shipment);
                if (!response.isSuccess()) {
                    log.error("shipment(id:{}) notify hk failed,cause:{}", response.getError());
                    Map<String, Object> param = Maps.newHashMap();
                    param.put("shipmentId", shipment.getId());
                    autoCompensateLogic.createAutoCompensationTask(param, TradeConstants.FAIL_SYNC_SHIPMENT_CONFIRM_TO_HK, response.getError());
                }
                continue;
            }
            //通知恒康已经发货
            Response<Boolean> response = syncErpShipmentLogic.syncShipmentDone(shipment, 2, MiddleOrderEvent.AUTO_HK_CONFIRME_FAILED.toOrderOperation());
            if (!response.isSuccess()) {
                log.error("notify hk order confirm failed,shipment id is ({}),caused by {}", shipment.getId(), response.getError());
            }
            Response<PoushengSettlementPos> r = poushengSettlementPosReadService.findByShipmentId(shipmentId);
            if (!r.isSuccess()) {
                log.error("failed find settlement pos, shipmentId={}, cause:{}", shipmentId, r.getError());
            }
            if (Objects.nonNull(r.getResult())) {
                PoushengSettlementPos pos = new PoushengSettlementPos();
                pos.setId(r.getResult().getId());
                pos.setPosDoneAt(new Date());
                Response<Boolean> rr = poushengSettlementPosWriteService.update(pos);
                if (!rr.isSuccess()) {
                    log.error("update pos done time failed,pousheng settlement pos id is {},caused by {}", r.getResult().getId(), rr.getError());
                }
            }
        }
    }

}
