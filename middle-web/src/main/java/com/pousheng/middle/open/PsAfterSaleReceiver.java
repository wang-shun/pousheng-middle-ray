package com.pousheng.middle.open;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.RefundItem;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.spu.service.PoushengMiddleSpuService;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.job.aftersale.component.DefaultAfterSaleReceiver;
import io.terminus.open.client.center.job.aftersale.dto.SkuOfRefund;
import io.terminus.open.client.order.dto.OpenClientAfterSale;
import io.terminus.open.client.order.enums.OpenClientAfterSaleStatus;
import io.terminus.open.client.order.enums.OpenClientAfterSaleType;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.RefundWriteService;
import io.terminus.parana.order.service.ShipmentReadService;
import io.terminus.parana.spu.model.SkuTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by cp on 7/17/17.
 */
@Component
@Slf4j
public class PsAfterSaleReceiver extends DefaultAfterSaleReceiver {

    private final PoushengMiddleSpuService middleSpuService;

    @RpcConsumer
    private RefundWriteService refundWriteService;
    @RpcConsumer
    private ShipmentReadService shipmentReadService;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    @Autowired
    public PsAfterSaleReceiver(PoushengMiddleSpuService middleSpuService) {
        this.middleSpuService = middleSpuService;
    }

    @Override
    protected void fillSkuInfo(ShopOrder shopOrder,Refund refund, SkuOfRefund skuOfRefund) {
        if (!StringUtils.hasText(skuOfRefund.getSkuCode())) {
            return;
        }

        Response<SkuTemplate> findR = middleSpuService.findBySkuCode(skuOfRefund.getSkuCode());
        if (!findR.isSuccess()) {
            log.error("fail to find sku template by skuCode={},cause:{}",
                    skuOfRefund.getSkuCode(), findR.getError());
            return;
        }
        SkuTemplate skuTemplate = findR.getResult();
        ReceiverInfo receiverInfo = orderReadLogic.findReceiverInfo(shopOrder.getId());
        SkuOrder skuOrder = orderReadLogic.findSkuOrderByShopOrderIdAndSkuCode(shopOrder.getId(),skuOfRefund.getSkuCode());
        //查询需要售后的发货单
        Shipment shipment = this.findShipmentByOrderInfo(shopOrder.getId(),skuOfRefund.getSkuCode(),skuOrder.getQuantity());
        RefundExtra refundExtra = new RefundExtra();
        refundExtra.setReceiverInfo(receiverInfo);
        if (!Objects.isNull(shipment)){
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
            refundExtra.setShipmentId(shipment.getId());
            refundExtra.setPosAmt(shipmentExtra.getPosAmt());
            refundExtra.setPosType(shipmentExtra.getPosType());
            refundExtra.setPosSerialNo(shipmentExtra.getPosSerialNo());
            refundExtra.setPosCreatedAt(shipmentExtra.getPosCreatedAt());
        }
        RefundItem refundItem = new RefundItem();
        if (!Objects.isNull(shipment)){
            List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
            ShipmentItem shipmentItem = shipmentItems
                    .stream().filter(shipmentItem1 ->
                            Objects.equals(shipmentItem1.getSkuCode(),skuOfRefund.getSkuCode())).collect(Collectors.toList()).get(0);
            refundItem.setFee(Long.valueOf(shipmentItem.getCleanFee()));
            refundItem.setSkuPrice(shipmentItem.getSkuPrice());
            refundItem.setSkuDiscount(shipmentItem.getSkuDiscount());
            refundItem.setCleanFee(shipmentItem.getCleanFee());
            refundItem.setCleanPrice(shipmentItem.getCleanPrice());
            refundItem.setAlreadyHandleNumber(shipmentItem.getQuantity());
            updateShipmentItemRefundQuantity(skuOfRefund.getSkuCode(),shipmentItem.getQuantity(),shipmentItems);
            //更新发货单商品中的已退货数量
            Map<String,String> shipmentExtraMap = shipment.getExtra();
            shipmentExtraMap.put(TradeConstants.SHIPMENT_ITEM_INFO,JsonMapper.nonEmptyMapper().toJson(shipmentItems));
            shipmentWiteLogic.updateExtra(shipment.getId(),shipmentExtraMap);

        }

        refundItem.setSkuCode(skuOrder.getSkuCode());
        refundItem.setOutSkuCode(skuOrder.getOutSkuId());
        refundItem.setAttrs(skuTemplate.getAttrs());
        refundItem.setSkuName(skuOrder.getItemName());
        refundItem.setApplyQuantity(skuOrder.getQuantity());
        Map<String,String> extraMap = Maps.newHashMap();
        extraMap.put(TradeConstants.REFUND_EXTRA_INFO,mapper.toJson(refundExtra));
        extraMap.put(TradeConstants.REFUND_ITEM_INFO,mapper.toJson(Lists.newArrayList(refundItem)));
        refund.setExtra(extraMap);
    }

    @Override
    protected Integer toParanaRefundType(OpenClientAfterSaleType type) {
        switch (type) {
            case IN_SALE_REFUND:
                return MiddleRefundType.ON_SALES_REFUND.value();
            case AFTER_SALE_ONLY_REFUND:
                return MiddleRefundType.AFTER_SALES_REFUND.value();
            case AFTER_SALE:
                return MiddleRefundType.AFTER_SALES_RETURN.value();
            case EXCHANGE:
                return MiddleRefundType.AFTER_SALES_CHANGE.value();
            default:
                return null;
        }
    }

    @Override
    protected Integer toParanaRefundStatus(OpenClientAfterSaleStatus status) {
        //TODO 转成中台对应的status
        switch (status){
            case SELLER_AGREE_BUYER:
                return MiddleRefundStatus.WAIT_HANDLE.getValue();
            case WAIT_BUYER_RETURN_GOODS:
                return MiddleRefundStatus.WAIT_HANDLE.getValue();
        }
        return null;
    }

    protected void updateRefund(Refund refund, OpenClientAfterSale afterSale) {
        if (afterSale.getStatus() != OpenClientAfterSaleStatus.SUCCESS) {
            return;
        }

        Response<Boolean> updateR = refundWriteService.updateStatus(refund.getId(), MiddleRefundStatus.REFUND.getValue());
        if (!updateR.isSuccess()) {
            log.error("fail to update refund(id={}) status to {} when receive after sale:{},cause:{}",
                    refund.getId(), MiddleRefundStatus.REFUND.getValue(), afterSale, updateR.getError());
        }
    }

    /**
     * 获取存在skuCode的发货单
     * @param shopOrderId 店铺订单id
     * @param skuCode 商品条码
     * @param quantity  申请售后的数量
     * @return
     */
    private Shipment findShipmentByOrderInfo(long shopOrderId,String skuCode,Integer quantity){
        Response<List<Shipment>> response = shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId, OrderLevel.SHOP);
        if (!response.isSuccess()){
            log.error("find shipment failed,shopOrderId is ({})",shopOrderId);
            throw new ServiceException("find.shipment.failed");
        }
        List<Shipment> shipments = response.getResult().stream().filter(Objects::nonNull).
                filter(shipment ->Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.SHIPPED.getValue())).collect(Collectors.toList());;
        for (Shipment shipment:shipments){
            List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
            List<ShipmentItem> shipmentItemFilters = shipmentItems.stream().
                    filter(Objects::nonNull).filter(shipmentItem -> Objects.equals(shipmentItem.getSkuCode(),skuCode))
                    .filter(shipmentItem -> (shipmentItem.getQuantity() >= quantity)).collect(Collectors.toList());
            if (shipmentItemFilters.size()>0){
                return shipment;
            }
        }
        return null;
    }

    //更新发货单商品中的已退货数量
    private void updateShipmentItemRefundQuantity(String skuCode,Integer refundQuantity,List<ShipmentItem> shipmentItems){
        for (ShipmentItem shipmentItem: shipmentItems){
            if(Objects.equals(skuCode,shipmentItem.getSkuCode())){
                shipmentItem.setRefundQuantity(shipmentItem.getRefundQuantity()+refundQuantity);
            }
        }
    }
}
