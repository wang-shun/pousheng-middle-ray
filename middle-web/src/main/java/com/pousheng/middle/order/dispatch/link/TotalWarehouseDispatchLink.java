package com.pousheng.middle.order.dispatch.link;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;
import com.pousheng.middle.hksyc.component.QueryHkWarhouseOrShopStockApi;
import com.pousheng.middle.hksyc.dto.item.HkSkuStockInfo;
import com.pousheng.middle.order.dispatch.component.DispatchComponent;
import com.pousheng.middle.order.dispatch.component.WarehouseAddressComponent;
import com.pousheng.middle.order.dispatch.contants.DispatchContants;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.*;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 总仓发货规则
 * 优先级 2
 * 1、判断上个规则传过来的仓信息中是否有总仓，无则进入下个规则
 * 2、过滤掉非mpos标签的仓，如果过滤后没有找到则进入下个规则
 * 3、调用roger的接口查询各个仓及仓下对应商品的库存
 * 4、判断是否有整单发货的仓，如果没有则进入下个规则，如果有则判断个数，如果只有一个则该仓发货，如果有多个则需要根据用户收货地址找出距离用户最近的一个仓
 * Created by songrenfei on 2017/12/22
 */
@Component
@Slf4j
public class TotalWarehouseDispatchLink implements DispatchOrderLink{

    @Autowired
    private WarehouseAddressComponent warehouseAddressComponent;
    @Autowired
    private QueryHkWarhouseOrShopStockApi queryHkWarhouseOrShopStockApi;
    @Autowired
    private DispatchComponent dispatchComponent;
    @Autowired
    private WarehouseCacher warehouseCacher;

    private static final Ordering<WarehouseWithPriority> byPriority = Ordering.natural().onResultOf(new Function<WarehouseWithPriority, Integer>() {
        @Override
        public Integer apply(WarehouseWithPriority input) {
            return input.getPriority();
        }
    });



    @Override
    public boolean dispatch(DispatchOrderItemInfo dispatchOrderItemInfo, ShopOrder shopOrder, ReceiverInfo receiverInfo, List<SkuCodeAndQuantity> skuCodeAndQuantities, Map<String, Serializable> context) throws Exception {
        log.info("DISPATCH-TotalWarehouseDispatchLink-2  order(id:{}) start...",shopOrder.getId());
        String companyCode = (String) context.get(DispatchContants.COMPANY_ID);
        Boolean oneCompany = (Boolean) context.get(DispatchContants.ONE_COMPANY);
        //收货地址明细
        String address = receiverInfo.getProvince() + receiverInfo.getCity() + receiverInfo.getRegion() + receiverInfo.getDetail();
        String region = Strings.isNullOrEmpty(receiverInfo.getRegion()) ? "" : receiverInfo.getRegion();
        String addressRegion = receiverInfo.getProvince() + receiverInfo.getCity() + region;
        context.put(DispatchContants.BUYER_ADDRESS,address);
        context.put(DispatchContants.BUYER_ADDRESS_REGION,addressRegion);

        Warehouses4Address warehouses4Address = (Warehouses4Address)context.get(DispatchContants.WAREHOUSE_FOR_ADDRESS);
        List<WarehouseWithPriority> totalWarehouseWithPriorities = warehouses4Address.getTotalWarehouses();

        //如果要求同公司则过滤掉其他公司的仓库，反之过滤掉同公司的
        if (oneCompany) {
            totalWarehouseWithPriorities = totalWarehouseWithPriorities.stream().filter(warehouse -> warehouseCacher.findById(warehouse.getWarehouseId()).getCompanyId().equals(companyCode)).collect(Collectors.toList());
        } else {
            totalWarehouseWithPriorities = totalWarehouseWithPriorities.stream().filter(warehouse -> !warehouseCacher.findById(warehouse.getWarehouseId()).getCompanyId().equals(companyCode)).collect(Collectors.toList());
        }
        //判断上个规则传过来的仓信息中是否有总仓，无则进入下个规则
        if(CollectionUtils.isEmpty(totalWarehouseWithPriorities)){
            return Boolean.TRUE;
        }

        //从上个规则传递过来。
        Table<Long, String, Integer> warehouseSkuCodeQuantityTable = (Table<Long, String, Integer>) context.get(DispatchContants.WAREHOUSE_SKUCODE_QUANTITY_TABLE);
        if(Arguments.isNull(warehouseSkuCodeQuantityTable)){
            warehouseSkuCodeQuantityTable = HashBasedTable.create();
            context.put(DispatchContants.WAREHOUSE_SKUCODE_QUANTITY_TABLE, (Serializable) warehouseSkuCodeQuantityTable);
        }

        List<Long> warehouseIds = Lists.transform(totalWarehouseWithPriorities, new Function<WarehouseWithPriority, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable WarehouseWithPriority input) {
                return input.getWarehouseId();
            }
        });

        List<WarehouseDTO> warehouses = warehouseAddressComponent.findWarehouseByIds(warehouseIds);

        //过滤掉非mpos仓
        /*List<Warehouse> mposWarehouses = warehouses.stream().filter(warehouse -> Objects.equal(warehouse.getIsMpos(),1)).filter(warehouse -> java.util.Objects.equals(warehouse.getType(),0)).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(mposWarehouses)){
            return Boolean.TRUE;
        }*/

        //查询仓代码
        List<String> stockCodes = dispatchComponent.getWarehouseOutCode(warehouses);

        List<String> skuCodes = dispatchComponent.getSkuCodes(skuCodeAndQuantities);


        List<HkSkuStockInfo> skuStockInfos = queryHkWarhouseOrShopStockApi.doQueryStockInfo(warehouseIds,skuCodes, dispatchOrderItemInfo.getOpenShopId());
        if(CollectionUtils.isEmpty(skuStockInfos)){
            return Boolean.TRUE;
        }

        // 放入 warehouseSkuCodeQuantityTable
        dispatchComponent.completeWarehouseTab(skuStockInfos,warehouseSkuCodeQuantityTable);

        //判断是否有整单

        List<WarehouseShipment> warehouseShipments = dispatchComponent.chooseSingleWarehouse(warehouseSkuCodeQuantityTable,skuCodeAndQuantities);

        //没有整单发的
        if(CollectionUtils.isEmpty(warehouseShipments)){
            return Boolean.TRUE;
        }


        //如果只有一个
        if(Objects.equal(warehouseShipments.size(),1)){
            dispatchOrderItemInfo.setWarehouseShipments(warehouseShipments);
            return Boolean.FALSE;
        }

        WarehouseShipment warehouseShipment = warehouseAddressComponent.nearestWarehouse(warehouses4Address.getPriorityWarehouseIds(), warehouseShipments, address, addressRegion);

        if(Arguments.isNull(warehouseShipment)){
            log.error("dispatch shop order(id:{}) fail,because query level fail",shopOrder.getId());
            throw new ServiceException("dispatch.fail");
        }

        dispatchOrderItemInfo.setWarehouseShipments(Lists.newArrayList(warehouseShipment));


        return Boolean.FALSE;

    }


}
