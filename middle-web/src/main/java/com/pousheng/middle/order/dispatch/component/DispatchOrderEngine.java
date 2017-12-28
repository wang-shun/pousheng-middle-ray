package com.pousheng.middle.order.dispatch.component;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.order.dispatch.dto.ShopShipment;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 派单引擎
 * Created by songrenfei on 2017/12/27
 */
@Component
@Slf4j
public class DispatchOrderEngine {

    @Autowired
    private ApplicationContext applicationContext;


    public Response<DispatchOrderItemInfo> toDispatchOrder(ShopOrder shopOrder, ReceiverInfo receiverInfo, List<SkuCodeAndQuantity> skuCodeAndQuantities){

        //因为这个的scope是prototype, 所以需要每次从容器中获取新实例
        DispatchLinkInvocation dispatchLinkInvocation = applicationContext.getBean(DispatchLinkInvocation.class);

        //初始化
        DispatchOrderItemInfo dispatchOrderItemInfo = new DispatchOrderItemInfo();
        List<ShopShipment> shopShipments = Lists.newArrayList();
        List<WarehouseShipment> warehouseShipments = Lists.newArrayList();
        dispatchOrderItemInfo.setShopShipments(shopShipments);
        dispatchOrderItemInfo.setWarehouseShipments(warehouseShipments);
        Map<String, Serializable> context = Maps.newHashMap();
        initDispatchOrderItemInfo(dispatchOrderItemInfo,context);
        try {
            boolean success = dispatchLinkInvocation.applyDispatchs(dispatchOrderItemInfo, shopOrder,receiverInfo,skuCodeAndQuantities, context);

            if(success){
                log.info("dispatchOrderItemInfo: " ,dispatchOrderItemInfo);
                return Response.ok(dispatchOrderItemInfo);
            }
            log.error("order:{} not matching any dispatch link",shopOrder.getId());
            return Response.fail("dispatch.order.fail");

        }catch (Exception e){
            log.error("dispatch order:{} fail,cause:{}",shopOrder.getId(), Throwables.getStackTraceAsString(e));
            return Response.fail("dispatch.order.fail");
        }
    }

    private void initDispatchOrderItemInfo(DispatchOrderItemInfo dispatchOrderItemInfo,Map<String, Serializable> context){


    }

}
