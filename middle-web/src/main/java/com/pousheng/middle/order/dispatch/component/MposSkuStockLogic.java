package com.pousheng.middle.order.dispatch.component;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.pousheng.middle.open.api.constant.ExtraKeyConstant;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.ShopShipment;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.manager.WarehouseSkuStockManager;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShipmentItem;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * mpos商品库存操作
 * mpos仓发的商品的库存全部放在WarehouseSkuStock,和电商公用
 * 在发货单发货锁定库存时 先拉取当前发货仓及商品对应的库存
 * 1、当没有电商在售时mpos在售则先拉对应的库存到WarehouseSkuStock，然后再进行锁定
 * 2、电商在售时，则同步最新的库存到WarehouseSkuStock后，然后再进行锁定
 * 3、锁定后对应的商品进行尝试推电商最新库存
 *
 * 仓发释放库存则直接释放即可
 * 仓发扣减库存则直接扣减即可
 *
 * Created by songrenfei on 2018/1/3
 */
@Component
@Slf4j
// TODO 店发shop全部转换成warehouse去库存交易，同时去掉mposstock操作，同时去掉同步门店库存数据
public class MposSkuStockLogic {
    @Autowired
    private WarehouseSkuStockManager warehouseSkuStockManager;
    @Autowired
    private WarehouseCacher warehouseCacher;
    @Autowired
    private ShopCacher shopCacher;
    @Autowired
    private DispatchComponent dispatchComponent;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private OrderReadLogic orderReadLogic;

    /**
     * 锁定库存
     * @param shipment 发货单信息
     * @param withSafe 是否使用安全库存
     */
    public Response<Boolean> lockStock(Shipment shipment, Boolean withSafe){
        DispatchOrderItemInfo dispatchOrderItemInfo = shipmentReadLogic.getDispatchOrderItem(shipment);
        return this.lockStock(dispatchOrderItemInfo, withSafe);

    }

    /**
     * 锁定库存
     * @param dispatchOrderItemInfo 商品信息
     */
    public Response<Boolean> lockStock(DispatchOrderItemInfo dispatchOrderItemInfo, Boolean withSafe){

        if(!isCareStock(dispatchOrderItemInfo.getOrderId())){
            return Response.ok();
        }

        //统一转成WarehouseShipment
        List<WarehouseShipment> warehouseShipments =  transToWarehouseShipment(dispatchOrderItemInfo);
        //如果没有仓发则直接返回
        if(CollectionUtils.isEmpty(warehouseShipments)){
            return Response.ok();
        }
        //锁定电商库存
        Response<Boolean> response;
        if (withSafe) {
            response = warehouseSkuStockManager.lockStockUserDispatch(dispatchComponent.genInventoryTradeDTO(dispatchOrderItemInfo), warehouseShipments);
        } else {
            response = warehouseSkuStockManager.lockStock(dispatchComponent.genInventoryTradeDTO(dispatchOrderItemInfo), warehouseShipments);
        }
        if(!response.isSuccess()){
            log.error("lock online warehouse sku stock:{} fail,error:{}",warehouseShipments,response.getError());
            return response;
        }

        return Response.ok();
    }


    /**
     * 解锁库存
     * @param shipment 商品信息
     */
    public Response<Boolean> unLockStock(Shipment shipment){


        DispatchOrderItemInfo dispatchOrderItemInfo = shipmentReadLogic.getDispatchOrderItem(shipment);
        if(!isCareStock(dispatchOrderItemInfo.getOrderId())){
            return Response.ok();
        }

        //仓库发货
        List<WarehouseShipment> warehouseShipments = dispatchOrderItemInfo.getWarehouseShipments();

        //没有说明不是仓发直接返回
        if(CollectionUtils.isEmpty(warehouseShipments)){
            return Response.ok();
        }
        warehouseSkuStockManager.unlockStock(dispatchComponent.genInventoryTradeDTO(dispatchOrderItemInfo), warehouseShipments);

        return Response.ok();

    }


    /**
     * 减少库存
     * @param shipment 发货单信息
     */
    public Response<Boolean> decreaseStock(Shipment shipment){

        DispatchOrderItemInfo dispatchOrderItemInfo = shipmentReadLogic.getDispatchOrderItem(shipment);
        if(!isCareStock(dispatchOrderItemInfo.getOrderId())){
            return Response.ok();
        }

        //仓库发货
        List<WarehouseShipment> warehouseShipments = dispatchOrderItemInfo.getWarehouseShipments();

        List<ShipmentItem> items = shipmentReadLogic.getShipmentItems(shipment);
        //没有说明不是仓发直接返回
        if(CollectionUtils.isEmpty(warehouseShipments)){
            return Response.ok();
        }

        warehouseSkuStockManager.decreaseStock(dispatchComponent.genInventoryTradeDTO(dispatchOrderItemInfo), warehouseShipments.get(0), items);

        log.info("end decrease stock");

        return Response.ok();
    }


    /**
     * 当前店铺下的订单是否关心库存
     * @param orderId 店铺id
     * @return true 关心 false 不关心
     */
    private Boolean isCareStock(Long orderId) {
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderId);

        if (shopOrder.getShopName().startsWith("yj")) {
            if (shopOrder.getExtra().containsKey(ExtraKeyConstant.IS_CARESTOCK)
                    && Objects.equals("Y", shopOrder.getExtra().get(ExtraKeyConstant.IS_CARESTOCK))) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    private List<WarehouseShipment> transToWarehouseShipment(DispatchOrderItemInfo dispatchOrderItemInfo){
        List<WarehouseShipment> warehouseShipments = Lists.newArrayList();
        warehouseShipments.addAll(dispatchOrderItemInfo.getWarehouseShipments());
        List<ShopShipment> shopShipments = dispatchOrderItemInfo.getShopShipments();
        if (!CollectionUtils.isEmpty(shopShipments)) {
            for (ShopShipment s : shopShipments) {
                Shop shop = shopCacher.findShopById(s.getShopId());
                WarehouseDTO warehouse = warehouseCacher.findByShopInfo(Joiner.on("_").join(Lists.newArrayList(shop.getOuterId(),shop.getBusinessId())));
                if (null == warehouse) {
                    log.error(" find warehouse by code {} is null ",shop.getOuterId() + "-_" + shop.getBusinessId());
                    throw new JsonResponseException("find.warehouse.failed");
                }
                WarehouseShipment warehouseShipment = new WarehouseShipment();
                warehouseShipment.setWarehouseId(warehouse.getId());
                warehouseShipment.setWarehouseName(warehouse.getWarehouseName());
                warehouseShipment.setSkuCodeAndQuantities(s.getSkuCodeAndQuantities());
                warehouseShipments.add(warehouseShipment);
            }
        }
        return warehouseShipments;
    }

}

