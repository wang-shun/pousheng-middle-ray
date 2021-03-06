package com.pousheng.middle.web.order.sync.erp;

import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.warehouse.companent.WarehouseClient;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.web.order.component.AutoCompensateLogic;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentPosLogic;
import com.pousheng.middle.web.order.sync.yjerp.SyncYJErpShipmentLogic;
import com.pousheng.middle.web.order.sync.yyedi.SyncWmsShipmentLogic;
import com.pousheng.middle.web.order.sync.yyedi.SyncYYEdiShipmentLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.open.client.order.enums.OpenClientStepOrderStatus;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 1.同步发货单信息到erp
 * 2.将已经同步到erp的发货单取消
 * Author: <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/15
 * pousheng-middle
 */
@Slf4j
@Component
public class SyncErpShipmentLogic {
    @Autowired
    private SyncShipmentLogic syncShipmentLogic;
    @Autowired
    private SyncYYEdiShipmentLogic syncYYEdiShipmentLogic;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @RpcConsumer
    private OpenShopReadService openShopReadService;
    @Autowired
    private SyncShipmentPosLogic syncShipmentPosLogic;
    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @Autowired
    private AutoCompensateLogic autoCompensateLogic;
    @Autowired
    private SyncYJErpShipmentLogic syncYJErpShipmentLogic;
    @Autowired
    private WarehouseClient warehouseClient;

    @Autowired
    private SyncWmsShipmentLogic syncWmsShipmentLogic;

    /**
     * 根据配置渠道确定将发货单同步到hk还是订单派发中心
     *
     * @param shipment
     * @return
     */
    public Response<Boolean> syncShipment(Shipment shipment) {

        log.info("sync shipment start,shipment is {}", shipment);

        OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShipment.getOrderId());


        //如果存在预售类型的订单，且预售类型的订单没有支付尾款，此时不能同步恒康
        Map<String, String> extraMap = shopOrder.getExtra();
        String isStepOrder = extraMap.get(TradeConstants.IS_STEP_ORDER);
        String stepOrderStatus = extraMap.get(TradeConstants.STEP_ORDER_STATUS);
        if (!StringUtils.isEmpty(isStepOrder) && Objects.equals(isStepOrder, "true")) {
            if (!StringUtils.isEmpty(stepOrderStatus) && Objects.equals(OpenClientStepOrderStatus.NOT_ALL_PAID.getValue(), Integer.valueOf(stepOrderStatus))) {
                log.info("this order is not all paid skit it order id is {}", shopOrder.getId());
                return Response.fail("this.order.is.not.all.paid");
            }
            if (!StringUtils.isEmpty(stepOrderStatus) && Objects.equals(OpenClientStepOrderStatus.NOT_PAID.getValue(), Integer.valueOf(stepOrderStatus))) {
                log.info("this order is not  paid skit it order id is {}", shopOrder.getId());
                return Response.fail("this.order.is.not.all.paid");
            }
        }

        // 云聚共享仓同步云聚erp
        if (shipment.getShipWay() == 2 && syncYJErp(shipment.getShipId())) {
            return syncYJErpShipmentLogic.syncShipmentToYJErp(shipment);
        }

        Response<OpenShop> openShopResponse = openShopReadService.findById(shopOrder.getShopId());
        if (!openShopResponse.isSuccess()) {
            log.error("find open shop by openShopId {} failed,caused by {}", shopOrder.getShopId(), openShopResponse.getError());
            return Response.fail(openShopResponse.getError());
        }
        OpenShop openShop = openShopResponse.getResult();
        Map<String, String> openShopExtra = openShop.getExtra();
        String erpSyncType = openShopExtra.get(TradeConstants.ERP_SYNC_TYPE) == null ? "hk" : openShopExtra.get(TradeConstants.ERP_SYNC_TYPE);
        switch (erpSyncType) {
            case "hk":
                return syncShipmentLogic.syncShipmentToHk(shipment,null);
            case "yyEdi":
                if (MiddleChannel.YUNJUJIT.getValue().equals(shopOrder.getOutFrom())) {
                    return syncWmsShipmentLogic.syncShipmentToWms(shipment);
                } else {
                    return syncYYEdiShipmentLogic.syncShipmentToYYEdi(shipment);
                }
            default:
                log.error("can not find sync erp type,openShopId is {}", shopOrder.getShopId());
                return Response.fail("find.open.shop.extra.erp.sync.type.fail");
        }
    }

    /**
     * 将已经同步到erp的发货单取消
     *
     * @param shipment 发货单
     * @return
     */
    public Response<Boolean> syncShipmentCancel(Shipment shipment, Object... skuOrders) {
        log.info("cancel shipment start,shipment is {}", shipment);
        OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShipment.getOrderId());
        // 云聚共享仓同步云聚erp
        if (shipment.getShipWay() == 2 && syncYJErp(shipment.getShipId())) {
            return syncYJErpShipmentLogic.syncShipmentCancelToYJErp(shipment);
        }
        Response<OpenShop> openShopResponse = openShopReadService.findById(shopOrder.getShopId());
        if (!openShopResponse.isSuccess()) {
            log.error("find open shop by openShopId {} failed,caused by {}", shopOrder.getShopId(), openShopResponse.getError());
        }
        OpenShop openShop = openShopResponse.getResult();
        Map<String, String> openShopExtra = openShop.getExtra();
        String erpSyncType = openShopExtra.get(TradeConstants.ERP_SYNC_TYPE) == null ? "hk" : openShopExtra.get(TradeConstants.ERP_SYNC_TYPE);
        switch (erpSyncType) {
            case "hk":
                return syncShipmentLogic.syncShipmentCancelToHk(shipment);
            case "yyEdi":
                return syncYYEdiShipmentLogic.syncShipmentCancelToYYEdi(shipment, skuOrders);
            default:
                log.error("can not find sync erp type,openShopId is {}", shopOrder.getShopId());
                return Response.fail("find.open.shop.extra.erp.sync.type.fail");
        }
    }

    /**
     * 自动同步发货单收货信息到erp
     *
     * @param shipment           发货单
     * @param operationType      0 取消 1 删除 2 收货状态更新
     * @param syncOrderOperation 同步失败的动作(手动和自动略有不同)
     * @return 同步结果, 同步成功true, 同步失败false
     */
    public Response<Boolean> syncShipmentDone(Shipment shipment, Integer operationType, OrderOperation syncOrderOperation) {
        log.info("sync shipment done start,shipment is {}", shipment);
        Response<Boolean> r = syncShipmentPosLogic.syncShipmentDoneToHk(shipment);
        if (r.isSuccess()) {
            OrderOperation operation = MiddleOrderEvent.HK_CONFIRMD_SUCCESS.toOrderOperation();
            Response<Boolean> updateStatus = shipmentWiteLogic.updateStatusLocking(shipment, operation);
            if (!updateStatus.isSuccess()) {
                log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), operation.getText(), updateStatus.getError());
                return Response.fail(updateStatus.getError());
            }
        } else {
            log.error("shipment(id:{}) notify hk failed,error:{}", shipment.getId(), r.getError());
            updateShipmetDoneToHkFail(shipment, MiddleOrderEvent.AUTO_HK_CONFIRME_FAILED.toOrderOperation());
            return Response.fail("恒康返回信息:" + r.getError());
        }
        return Response.ok(Boolean.TRUE);
    }

    private void updateShipmetDoneToHkFail(Shipment shipment, OrderOperation syncOrderOperation) {
        Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatusLocking(shipment, syncOrderOperation);
        if (!updateSyncStatusRes.isSuccess()) {
            //这里失败只打印日志即可
            log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
        }
    }


    private Boolean syncYJErp (Long warehouseId) {
        Response<WarehouseDTO> rW  = warehouseClient.findById(warehouseId);
        if (!rW.isSuccess()){
            throw new ServiceException("find.warehouse.failed");
        }
        WarehouseDTO warehouse = rW.getResult();
        Map<String, String> extra = warehouse.getExtra();
        if (extra.containsKey(TradeConstants.IS_SHARED_STOCK) && Objects.equals(extra.get(TradeConstants.IS_SHARED_STOCK), "Y")) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

}
