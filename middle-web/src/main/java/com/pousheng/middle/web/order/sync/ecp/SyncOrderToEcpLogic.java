package com.pousheng.middle.web.order.sync.ecp;

import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.enums.SyncTaobaoStatus;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.OrderWriteLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.open.client.center.order.service.OrderServiceCenter;
import io.terminus.open.client.order.dto.OpenClientOrderShipment;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 同步电商发货单逻辑
 * Created by tony on 2017/7/5.
 * pousheng-middle
 */
@Slf4j
@Component
public class SyncOrderToEcpLogic {
    @Autowired
    private OrderWriteLogic orderWriteLogic;
    @Autowired
    private OrderServiceCenter orderServiceCenter;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;

    /**
     * 同步发货单到电商
     * @param shopOrder
     * @param expressCompayCode
     * @param shipmentId
     * @return
     */
    public Response<Boolean> syncOrderToECP(ShopOrder shopOrder,String expressCompayCode,Long shipmentId)
    {
        //更新状态为同步中
        try {
            Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
            OrderOperation orderOperation = MiddleOrderEvent.SYNC_ECP.toOrderOperation();
            orderWriteLogic.updateEcpOrderStatus(shopOrder, orderOperation);
            OpenClientOrderShipment orderShipment = new OpenClientOrderShipment();
            orderShipment.setOuterOrderId(shopOrder.getOutId());
            orderShipment.setLogisticsCompany(expressCompayCode);
            //填写运单号
            orderShipment.setWaybill(String.valueOf(shipmentExtra.getShipmentSerialNo()));

            Response<Boolean> response = orderServiceCenter.ship(shopOrder.getShopId(), orderShipment);
            if (response.isSuccess()) {
                //同步成功
                OrderOperation successOperation = MiddleOrderEvent.SYNC_SUCCESS.toOrderOperation();
                orderWriteLogic.updateEcpOrderStatus(shopOrder, successOperation);
            } else {
                //同步失败
                OrderOperation failOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
                orderWriteLogic.updateEcpOrderStatus(shopOrder, failOperation);
                return Response.fail("sync.ecp.fail");
            }
        }catch (Exception e) {
            log.error("sync ecp failed,shopOrderId is({}),cause by {}", shopOrder.getId(), e.getMessage());
            OrderOperation failOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
            orderWriteLogic.updateEcpOrderStatus(shopOrder, failOperation);
        }

        return Response.ok();
    }



    /**
     * 同步发货单到电商
     * @param shopOrder
     * @return
     */
    public Response<Boolean> syncOrderToTaobao(ShopOrder shopOrder)
    {
        //更新状态为同步中
        OrderOperation orderOperation = MiddleOrderEvent.SYNC_ECP.toOrderOperation();
        orderWriteLogic.updateEcpOrderStatus(shopOrder, orderOperation);

        try {
            List<OrderShipment> orderShipments = shipmentReadLogic.findByOrderIdAndType(shopOrder.getId());
            List<OrderShipment> orderShipmentsFilter = orderShipments.stream().filter(Objects::nonNull)
                    .filter(it->!Objects.equals(MiddleShipmentsStatus.CANCELED.getValue(),it.getStatus())).collect(Collectors.toList());
            int count = 0;//判断是否存在同步淘宝失败的发货单
            for (OrderShipment orderShipment:orderShipmentsFilter){
                Shipment shipment = shipmentReadLogic.findShipmentById(Long.valueOf(orderShipment.getShipmentId()));
                ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
                try{
                    //获取当前同步淘宝的状态
                    Integer syncTaobaoStatus = shipmentExtra.getSyncTaobaoStatus()==null? SyncTaobaoStatus.WAIT_SYNC_TAOBAO.getValue()
                            :shipmentExtra.getSyncTaobaoStatus();
                    //如果存在已经同步淘宝成功的发货单,则跳过
                    if (Objects.equals(syncTaobaoStatus,SyncTaobaoStatus.SYNC_TAOBAO_SUCCESS.getValue())){
                        continue;
                    }
                    //获取快递信息
                    ExpressCode expressCode = orderReadLogic.makeExpressNameByhkCode(shipmentExtra.getShipmentCorpCode());
                    String expressCompanyCode = orderReadLogic.getExpressCode(shopOrder.getShopId(), expressCode);

                    OpenClientOrderShipment openClientOrderShipment = new OpenClientOrderShipment();
                    openClientOrderShipment.setOuterOrderId(shopOrder.getOutId());
                    openClientOrderShipment.setLogisticsCompany(expressCompanyCode);
                    //填写运单号
                    openClientOrderShipment.setWaybill(String.valueOf(shipmentExtra.getShipmentSerialNo()));
                    Response<Boolean> response = orderServiceCenter.ship(shopOrder.getShopId(), openClientOrderShipment);
                    if (response.isSuccess()){
                        shipmentWiteLogic.updateShipmentSyncTaobaoStatus(shipment,MiddleOrderEvent.SYNC_TAOBAO_SUCCESS.toOrderOperation());
                    }else{
                        count++;
                        shipmentWiteLogic.updateShipmentSyncTaobaoStatus(shipment,MiddleOrderEvent.SYNC_TAOBAO_FAIL.toOrderOperation());
                    }
                }catch (Exception e){
                    log.error("sync shipment to taobao failed,shipmentId is {},caused by {}",shipment.getId(),e.getMessage());
                    shipmentWiteLogic.updateShipmentSyncTaobaoStatus(shipment,MiddleOrderEvent.SYNC_TAOBAO_FAIL.toOrderOperation());
                    throw new ServiceException(e.getMessage());
                }
            }

            if (count==0) {
                //同步成功
                OrderOperation successOperation = MiddleOrderEvent.SYNC_SUCCESS.toOrderOperation();
                orderWriteLogic.updateEcpOrderStatus(shopOrder, successOperation);
            } else {
                //同步失败
                OrderOperation failOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
                orderWriteLogic.updateEcpOrderStatus(shopOrder, failOperation);
                return Response.fail("sync.ecp.fail");
            }
        }catch (Exception e) {
            log.error("sync ecp failed,shopOrderId is({}),cause by {}", shopOrder.getId(), e.getMessage());
            OrderOperation failOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
            orderWriteLogic.updateEcpOrderStatus(shopOrder, failOperation);
        }

        return Response.ok();
    }
}
