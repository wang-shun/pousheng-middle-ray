package com.pousheng.middle.open.api;

import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.open.api.dto.HkHandleShipmentResult;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ExpressCodeCriteria;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.EcpOrderStatus;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.order.service.ExpressCodeReadService;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import com.pousheng.middle.web.events.trade.HkShipmentDoneEvent;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.order.sync.ecp.SyncOrderToEcpLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.order.service.RefundWriteService;
import io.terminus.parana.order.service.ShipmentWriteService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 订单open api
 * Created by songrenfei on 2017/6/15
 */
@OpenBean
@Slf4j
public class OrderOpenApi {

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @RpcConsumer
    private ShipmentWriteService shipmentWriteService;
    @Autowired
    private MiddleOrderFlowPicker flowPicker;
    @RpcConsumer
    private ExpressCodeReadService expressCodeReadService;
    @RpcConsumer
    private OrderWriteLogic orderWriteLogic;
    @RpcConsumer
    private OrderWriteService orderWriteService;
    @RpcConsumer
    private OrderReadLogic orderReadLogic;
    @RpcConsumer
    private RefundWriteLogic refundWriteLogic;
    @RpcConsumer
    private RefundReadLogic refundReadLogic;
    @RpcConsumer
    private RefundWriteService refundWriteService;
    @RpcConsumer
    private OrderShipmentReadService orderShipmentReadService;
    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @RpcConsumer
    private OpenShopReadService openShopReadService;
    @Autowired
    private SyncOrderToEcpLogic syncOrderToEcpLogic;
    @Autowired
    private EventBus eventBus;


    private final static DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyyMMddHHmmss");




    /**
     * 恒康同步发货单处理结果
     *
     * @param results 处理结果
     * @return 是否同步成功
     */
    @OpenMethod(key = "hk.shipment.handle.result", paramNames = {"data"}, httpMethods = RequestMethod.POST)
    public void syncHkHandleResult(@NotNull(message = "handle.data.is.null") List<HkHandleShipmentResult> results) {
        log.info("HK-SYNC-SHIPMENT-HANDLE-RESULT-START results is:{} ",results);
        for (HkHandleShipmentResult result :results){
            try{
                Long shipmentId = result.getEcShipmentId();
                Boolean handleResult = result.getSuccess();
                Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
                //更新发货单的状态
                if (handleResult){
                    OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_SUCCESS.toOrderOperation();
                    Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatus(shipment, syncOrderOperation);
                    if (!updateSyncStatusRes.isSuccess()) {
                        log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
                    }
                }else{
                    OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
                    Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatus(shipment, syncOrderOperation);
                    if (!updateSyncStatusRes.isSuccess()) {
                        log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
                    }
                }

            }catch (JsonResponseException | ServiceException e) {
                log.error("hk shipment handle result, shipment(id:{}) to pousheng fail,error:{}", results.get(0).getEcShipmentId(), e.getMessage());
                throw new OPServerException(200,e.getMessage());
            }
            catch (Exception e){
                log.error("hk shipment handle result ,shipment(id:{}) fail,cause:{}", results.get(0).getEcShipmentId(), Throwables.getStackTraceAsString(e));
                throw new OPServerException(200,"sync.fail");
            }
        }

        log.info("HK-SYNC-SHIPMENT-HANDLE-RESULT-END");
    }

            /**
             * 恒康同步发货完成状态到中台
             *
             * @param shipmentId       中台发货单号
             * @param hkShipmentId     恒康发货单号
             * @param shipmentCorpCode 物流公司代码
             * @param shipmentSerialNo 物流单号
             * @param shipmentDate     发货时间
             * @return 是否同步成功
             */
    @OpenMethod(key = "hk.shipments.api", paramNames = {"shipmentId", "hkShipmentId",
                                                         "shipmentCorpCode", "shipmentSerialNo",
                                                         "shipmentDate","posSerialNo","posType",
                                                         "posAmt","posCreatedAt"}, httpMethods = RequestMethod.POST)
    public void syncHkShipmentStatus(@NotNull(message = "shipment.id.is.null") Long shipmentId,
                                     @NotEmpty(message = "hk.shipment.id.is.null") String hkShipmentId,
                                     @NotEmpty(message = "shipment.corp.code.empty") String shipmentCorpCode,
                                     @NotEmpty(message = "shipment.serial.is.empty") String shipmentSerialNo,
                                     @NotEmpty(message = "shipment.date.empty") String shipmentDate,
                                     @NotEmpty(message = "pos.serial.is.empty")String posSerialNo,
                                     @NotNull(message = "pos.type.is.null")Integer posType,
                                     @NotNull(message = "pos.amt.is.empty")String posAmt,
                                     @NotEmpty(message = "pos.created.time.is.empty")String posCreatedAt) {
        log.info("HK-SYNC-SHIPMENT-STATUS-START param shipmentId is:{} hkShipmentId is:{} shipmentCorpCode is:{} " +
                "shipmentSerialNo is:{} shipmentDate is:{} posSerialNo is:{} posType is:{} posAmt is:{} posCreatedAt is:{}",
                shipmentId, hkShipmentId, shipmentCorpCode, shipmentSerialNo, shipmentDate,posSerialNo,posType,posAmt,posCreatedAt);

        try {

            DateTime dt = DateTime.parse(shipmentDate, DFT);
            DateTime dPos = DateTime.parse(posCreatedAt, DFT);
           Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
            //判断状态及获取接下来的状态
            Flow flow = flowPicker.pickShipments();
            OrderOperation orderOperation = MiddleOrderEvent.SHIP.toOrderOperation();
            if (!flow.operationAllowed(shipment.getStatus(), orderOperation)) {
                log.error("shipment(id={})'s status({}) not fit for ship",
                        shipment.getId(), shipment.getStatus());
                throw new ServiceException("shipment.current.status.not.allow.ship");
            }
            Integer targetStatus = flow.target(shipment.getStatus(), orderOperation);
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);

            if (!Objects.equals(hkShipmentId, shipmentExtra.getOutShipmentId())) {
                log.error("hk shipment id:{} not equal middle shipment(id:{} ) out shipment id:{}", hkShipmentId, shipment.getId(), shipmentExtra.getOutShipmentId());
                throw new ServiceException("hk.shipment.id.not.matching");
            }


            //封装更新信息
            Shipment update = new Shipment();
            update.setId(shipment.getId());
            Map<String, String> extraMap = shipment.getExtra();
            shipmentExtra.setShipmentSerialNo(shipmentSerialNo);
            shipmentExtra.setShipmentCorpCode(shipmentCorpCode);
            //通过恒康代码查找快递名称
            ExpressCode expressCode = makeExpressNameByhkCode(shipmentCorpCode);
            shipmentExtra.setShipmentCorpName(expressCode.getName());
            shipmentExtra.setShipmentDate(dt.toDate());
            //添加pos单相关信息
            shipmentExtra.setPosSerialNo(posSerialNo);
            shipmentExtra.setPosType(String.valueOf(posType));
            shipmentExtra.setPosAmt(String.valueOf(posAmt));
            shipmentExtra.setPosCreatedAt(dPos.toDate());
            extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, mapper.toJson(shipmentExtra));
            update.setExtra(extraMap);

            //更新状态
            Response<Boolean> updateStatusRes = shipmentWriteService.updateStatusByShipmentId(shipment.getId(), targetStatus);
            if (!updateStatusRes.isSuccess()) {
                log.error("update shipment(id:{}) status to :{} fail,error:{}", shipment.getId(), targetStatus, updateStatusRes.getError());
                throw new ServiceException(updateStatusRes.getError());
            }

            //更新基本信息
            Response<Boolean> updateRes = shipmentWriteService.update(update);
            if (!updateRes.isSuccess()) {
                log.error("update shipment(id:{}) extraMap to :{} fail,error:{}", shipment.getId(), extraMap, updateRes.getError());
                throw new ServiceException(updateStatusRes.getError());
            }
            //恒康第一个发货单发货完成之后需要将ecpOrderstatus状态从初始的代发货修改为已发货
            OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
            long orderShopId = orderShipment.getOrderId();
            ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShopId);
            String status = orderReadLogic.getOrderExtraMapValueByKey(TradeConstants.ECP_ORDER_STATUS, shopOrder);
            //判断ecpOrder的状态是否是初始的待发货状态,如果不是,跳过
            if (Objects.equals(Integer.valueOf(status), EcpOrderStatus.WAIT_SHIP.getValue())) {
                Response<Boolean> response = orderWriteLogic.updateEcpOrderStatus(shopOrder, MiddleOrderEvent.SHIP.toOrderOperation());
                if (!response.isSuccess()){
                    log.error("update shopOrder(id:{}) extraMap fail,error:{}",orderShopId,response.getError());
                    throw new ServiceException(response.getError());
                }
                //冗余shipmentId到extra中
                Map<String, String> extraMap1 = shopOrder.getExtra();
                extraMap1.put(TradeConstants.ECP_SHIPMENT_ID, String.valueOf(shipmentId));
                Response<Boolean> response1 = orderWriteService.updateOrderExtra(shopOrder.getId(), OrderLevel.SHOP,extraMap1);
                if (!response1.isSuccess()) {
                    log.error("update shopOrder：{} extra map to:{} fail,error:{}", shopOrder.getId(), extraMap, response1.getError());
                    throw new ServiceException(response1.getError());
                }
                //第一个发货单发货完成之后需要将订单同步
                String expressCompanyCode = orderReadLogic.getExpressCode(shopOrder.getShopId(),expressCode);
                syncOrderToEcpLogic.syncOrderToECP(shopOrder,expressCompanyCode,shipmentId);
            }
            //使用一个监听事件,用来监听是否存在订单或者售后单下的发货单是否已经全部发货完成
            HkShipmentDoneEvent event = new HkShipmentDoneEvent();
            event.setShipment(shipment);
            eventBus.post(event);

        } catch (JsonResponseException | ServiceException e) {
            log.error("hk sync shipment(id:{}) to pousheng fail,error:{}", shipmentId, e.getMessage());
            throw new OPServerException(200,e.getMessage());
        } catch (Exception e) {
            log.error("hk sync shipment(id:{}) fail,cause:{}", shipmentId, Throwables.getStackTraceAsString(e));
            throw new OPServerException(200,"sync.fail");
        }

        log.info("HK-SYNC-SHIPMENT-STATUS-END");
    }

    /**
     * 恒康将售后单售后结果通知给中台
     * @param refundOrderId
     * @param hkRefundOrderId
     * @param itemInfo
     * @param receivedDate
     * @param posSerialNo
     * @param posType
     * @param posAmt
     * @param posCreatedAt
     */
    @OpenMethod(key = "hk.refund.confirm.received.api", paramNames = {"refundOrderId", "hkRefundOrderId", "itemInfo",
                                                                      "receivedDate","posSerialNo","posType",
                                                                      "posAmt","posCreatedAt"}, httpMethods = RequestMethod.POST)
    public void syncHkRefundStatus(@NotNull(message = "refund.order.id.is.null") Long refundOrderId,
                                   @NotEmpty(message = "hk.refund.order.id.is.null") String hkRefundOrderId,
                                   @NotEmpty(message = "item.info.empty") String itemInfo,
                                   @NotEmpty(message = "received.date.empty") String receivedDate,
                                   @NotEmpty(message = "pos.serial.is.empty")String posSerialNo,
                                   @NotNull(message = "pos.type.is.null")Integer posType,
                                   @NotNull(message = "pos.amt.is.empty")String posAmt,
                                   @NotEmpty(message = "pos.created.time.is.empty")String posCreatedAt) {
        log.info("HK-SYNC-REFUND-STATUS-START param refundOrderId is:{} hkRefundOrderId is:{} itemInfo is:{}  posSerialNo is:{} posType is:{} posAmt is:{} posCreatedAt is:{} " +
                "shipmentDate is:{}", refundOrderId, hkRefundOrderId, itemInfo, receivedDate,posSerialNo,posType,posAmt,posCreatedAt);
        try {
           Refund refund = refundReadLogic.findRefundById(refundOrderId);
            if (!Objects.equals(hkRefundOrderId, refund.getOutId())) {
                log.error("hk refund id:{} not equal middle refund(id:{} ) out id:{}", hkRefundOrderId, refund.getId(), refund.getOutId());
                throw new ServiceException("hk.refund.id.not.matching");
            }

            //todo 恒康返回的商品信息如何处理

            DateTime dt = DateTime.parse(receivedDate, DFT);

            RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
            refundExtra.setHkReturnDoneAt(dt.toDate());
            //refundExtra.setHkConfirmItemInfo(itemInfo);

            //更新状态
            OrderOperation orderOperation = getSyncConfirmSuccessOperation(refund);
            Response<Boolean> updateStatusRes = refundWriteLogic.updateStatus(refund, orderOperation);
            if (!updateStatusRes.isSuccess()) {
                log.error("update refund(id:{}) status,operation:{} fail,error:{}", refund.getId(), orderOperation.getText(), updateStatusRes.getError());
                throw new ServiceException(updateStatusRes.getError());
            }

            //更新扩展信息
            Refund update = new Refund();
            update.setId(refundOrderId);
            Map<String, String> extraMap = refund.getExtra();
            extraMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
            update.setExtra(extraMap);

            Response<Boolean> updateExtraRes = refundWriteLogic.update(update);
            if (!updateExtraRes.isSuccess()) {
                log.error("update rMatrixRequestHeadefund(id:{}) extra:{} fail,error:{}", refundOrderId, refundExtra, updateExtraRes.getError());
                //这就就不抛出错了，中台自己处理即可。
            }

        } catch (JsonResponseException | ServiceException e) {
            log.error("hk sync refund confirm to middle fail,error:{}", e.getMessage());
            throw new OPServerException(200,e.getMessage());
        } catch (Exception e) {
            log.error("hk sync refund confirm to middle fail,cause:{}", Throwables.getStackTraceAsString(e));
            throw new OPServerException(200,"sync.fail");
        }
        log.info("HK-SYNC-REFUND-STATUS-END");

    }




    public ExpressCode makeExpressNameByhkCode(String hkExpressCode) {
        ExpressCodeCriteria criteria = new ExpressCodeCriteria();
        criteria.setHkCode(hkExpressCode);
        Response<Paging<ExpressCode>> response = expressCodeReadService.pagingExpressCode(criteria);
        if (!response.isSuccess()) {
            log.error("failed to pagination expressCode with criteria:{}, error code:{}", criteria, response.getError());
            throw new JsonResponseException(response.getError());
        }
        if (response.getResult().getData().size() == 0) {
            log.error("there is not any express info by hkCode:{}", hkExpressCode);
            throw new JsonResponseException("express.info.is.not.exist");
        }
        ExpressCode expressCode = response.getResult().getData().get(0);
        return expressCode;
    }


    //获取同步成功事件
    private OrderOperation getSyncConfirmSuccessOperation(Refund refund) {
        MiddleRefundType middleRefundType = MiddleRefundType.from(refund.getRefundType());
        if (Arguments.isNull(middleRefundType)) {
            log.error("refund(id:{}) type:{} invalid", refund.getId(), refund.getRefundType());
            throw new JsonResponseException("refund.type.invalid");
        }

        switch (middleRefundType) {
            case AFTER_SALES_RETURN:
                return MiddleOrderEvent.RETURN.toOrderOperation();
            case AFTER_SALES_REFUND:
                log.error("refund(id:{}) type:{} not allow hk confirm", refund.getId(), refund.getRefundType());
                throw new JsonResponseException("refund.not.allow.hk.confirm");
            case AFTER_SALES_CHANGE:
                return MiddleOrderEvent.RETURN_CHANGE.toOrderOperation();
            default:
                log.error("refund(id:{}) type:{} invalid", refund.getId(), refund.getRefundType());
                throw new JsonResponseException("refund.type.invalid");
        }

    }
}
