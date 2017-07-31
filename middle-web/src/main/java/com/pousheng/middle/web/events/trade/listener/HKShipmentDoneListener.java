package com.pousheng.middle.web.events.trade.listener;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.EcpOrderStatus;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import com.pousheng.middle.warehouse.service.WarehouseSkuWriteService;
import com.pousheng.middle.web.events.trade.HkShipmentDoneEvent;
import com.pousheng.middle.web.order.component.*;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.enums.ShipmentType;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.order.service.RefundWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 一旦订单或者售后单下面的发货单已经全部发货,更新订单或者发货单的状态为已经发货
 * Created by tony on 2017/7/10.
 * pousheng-middle
 */
@Slf4j
@Component
public class HKShipmentDoneListener {
    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();
    @Autowired
    private OrderShipmentReadService orderShipmentReadService;
    @Autowired
    private OrderWriteService orderWriteService;

    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private OrderWriteLogic orderWriteLogic;
    @Autowired
    private RefundWriteLogic refundWriteLogic;
    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private RefundWriteService refundWriteService;
    @Autowired
    private WarehouseSkuWriteService warehouseSkuWriteService;
    @Autowired
    private MiddleOrderFlowPicker flowPicker;
    @Autowired
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void doneShipment(HkShipmentDoneEvent event) {
        Shipment shipment = event.getShipment();
        //判断发货单是否发货完
        if (shipment.getType() == ShipmentType.SALES_SHIP.value()) {
            //判断发货单是否已经全部发货完成,如果全部发货完成之后需要更新order的状态为待收货
            OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
            long orderShopId = orderShipment.getOrderId();
            ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShopId);
            if (shopOrder.getStatus() == MiddleOrderStatus.WAIT_SHIP.getValue()) {
                List<OrderShipment> orderShipments = shipmentReadLogic.findByOrderIdAndType(orderShopId);
                List<Integer> orderShipMentStatusList = Lists.transform(orderShipments, new Function<OrderShipment, Integer>() {
                    @Nullable
                    @Override
                    public Integer apply(@Nullable OrderShipment orderShipment) {
                        return orderShipment.getStatus();
                    }
                });

                //判断此时是否还有处于待发货,待通知恒康,通知恒康中的发货单;如果没有,则此时店铺订单状态应该更新为已发货,sku订单更新状态时应该考虑排除已取消订单状态的更新
                if (!orderShipMentStatusList.contains(MiddleShipmentsStatus.WAIT_SHIP.getValue())
                        && !orderShipMentStatusList.contains(MiddleShipmentsStatus.SYNC_HK_ING.getValue()) &&
                        !orderShipMentStatusList.contains(MiddleShipmentsStatus.WAIT_SYNC_HK.getValue())) {
                    //待发货--商家已经发货
                    List<SkuOrder> skuOrders = orderReadLogic.findSkuOrderByShopOrderIdAndStatus(orderShopId, MiddleOrderStatus.WAIT_SHIP.getValue());
                    for (SkuOrder skuOrder : skuOrders) {
                        Response<Boolean> updateRlt = orderWriteService.skuOrderStatusChanged(skuOrder.getId(), MiddleOrderStatus.WAIT_SHIP.getValue(), MiddleOrderStatus.SHIPPED.getValue());
                        if (!updateRlt.getResult()) {
                            log.error("update skuOrder status error (id:{}),original status is {}", skuOrder.getId(), skuOrder.getStatus());
                            throw new JsonResponseException("update.sku.order.status.error");
                        }
                    }
                    //此时判断EcpOrderStatus的状态,如果ecpOrderStatus是已收货,直接将订单表状态更新为已经完成
                    String ecpOrderStatus = orderReadLogic.getOrderExtraMapValueByKey(TradeConstants.ECP_ORDER_STATUS, shopOrder);
                    if (Objects.equals(Integer.valueOf(ecpOrderStatus), EcpOrderStatus.CONFIRMED.getValue())) {
                        Response<Boolean> updateRes = orderWriteService.shopOrderStatusChanged(shopOrder.getId(), MiddleOrderStatus.SHIPPED.getValue(), MiddleOrderStatus.CONFIRMED.getValue());
                        if (!updateRes.getResult()) {
                            log.error("update shopOrder status error (id:{}),original status is {}", shopOrder.getId(), MiddleOrderStatus.SHIPPED.getValue());
                            throw new JsonResponseException("update.shop.order.status.error");
                        }
                    }

                }
            }

        }
        if (shipment.getType() == ShipmentType.EXCHANGE_SHIP.value()) {
            //如果发货单已经全部发货完成,需要更新refund表的状态为待确认收货,rufund表的状态为待收货完成
            Response<OrderShipment> orderShipmentResponse = orderShipmentReadService.findByShipmentId(shipment.getId());
            OrderShipment orderShipment = orderShipmentResponse.getResult();
            long afterSaleOrderId = orderShipment.getAfterSaleOrderId();
            Refund refund = refundReadLogic.findRefundById(afterSaleOrderId);
            if (refund.getStatus() == MiddleRefundStatus.WAIT_SHIP.getValue()) {
                Response<List<OrderShipment>> listResponse = orderShipmentReadService.findByAfterSaleOrderIdAndOrderLevel(afterSaleOrderId, OrderLevel.SHOP);
                List<Integer> orderShipMentStatusList = Lists.transform(listResponse.getResult(), new Function<OrderShipment, Integer>() {
                    @Nullable
                    @Override
                    public Integer apply(@Nullable OrderShipment orderShipment) {
                        return orderShipment.getStatus();
                    }
                });
                if (!orderShipMentStatusList.contains(MiddleShipmentsStatus.WAIT_SHIP.getValue())
                        && !orderShipMentStatusList.contains(MiddleShipmentsStatus.SYNC_HK_ING.getValue()) &&
                        !orderShipMentStatusList.contains(MiddleShipmentsStatus.WAIT_SYNC_HK.getValue())) {
                    //更新售后单的处理状态

                    Response<Boolean> resRlt = refundWriteLogic.updateStatus(refund, MiddleOrderEvent.SHIP.toOrderOperation());
                    if (!resRlt.isSuccess()) {
                        log.error("update refund status error (id:{}),original status is {}", refund.getId(), refund.getStatus());
                        throw new JsonResponseException("update.refund.status.error");
                    }
                    //将shipmentExtra的已发货时间塞入值
                    RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
                    refundExtra.setShipAt(new Date());
                    Map<String, String> extrMap = refund.getExtra();
                    extrMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
                    refund.setExtra(extrMap);
                    Response<Boolean> updateRefundRes = refundWriteService.update(refund);
                    if (!updateRefundRes.isSuccess()) {
                        log.error("update refund(id:{}) fail,error:{}", refund, updateRefundRes.getError());
                        throw new JsonResponseException("update.refund.error");
                    }
                }
            }
        }
    }
}
