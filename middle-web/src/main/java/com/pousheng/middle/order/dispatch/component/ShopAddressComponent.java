package com.pousheng.middle.order.dispatch.component;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.gd.GDMapSearchService;
import com.pousheng.middle.gd.Location;
import com.pousheng.middle.order.cache.AddressGpsCacher;
import com.pousheng.middle.order.dispatch.dto.DistanceDto;
import com.pousheng.middle.order.enums.AddressBusinessType;
import com.pousheng.middle.order.model.AddressGps;
import com.pousheng.middle.order.service.AddressGpsReadService;
import com.pousheng.middle.warehouse.dto.ShopShipment;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by songrenfei on 2017/12/25
 */
@Component
@Slf4j
public class ShopAddressComponent {


    @Autowired
    private AddressGpsReadService addressGpsReadService;
    @RpcConsumer
    private ShopReadService shopReadService;
    @Autowired
    private GDMapSearchService gdMapSearchService;
    @Autowired
    private AddressGpsCacher addressGpsCacher;
    @Autowired
    private DispatchComponent dispatchComponent;




    public List<AddressGps> findShopAddressGps(Long provinceId){

        Response<List<AddressGps>> addressGpsListRes = addressGpsReadService.findByProvinceIdAndBusinessType(provinceId, AddressBusinessType.SHOP);
        if(!addressGpsListRes.isSuccess()){
            log.error("find addressGps by province id :{} for shop failed,  error:{}", provinceId,addressGpsListRes.getError());
            throw new ServiceException(addressGpsListRes.getError());
        }
        return addressGpsListRes.getResult();

    }


    public List<Shop> findShopByIds(List<Long> ids){

        Response<List<Shop>> shopListRes = shopReadService.findByIds(ids);
        if(!shopListRes.isSuccess()){
            log.error("find shop by ids:{} failed,  error:{}", ids,shopListRes.getError());
            throw new ServiceException(shopListRes.getError());
        }
        return shopListRes.getResult();

    }


    /**
     * 获取距离用户收货地址最近的门店
     * @param shopShipments 发货门店集合
     * @param address 用户收货地址
     * @param addressRegion 用户收货地址到区
     * @return 距离最近的发货门店
     */
    public ShopShipment getNearest(List<ShopShipment> shopShipments, String address,String addressRegion){

        Location location = dispatchComponent.getLocation(address,addressRegion);

        List<DistanceDto> distanceDtos = Lists.newArrayListWithCapacity(shopShipments.size());
        for (ShopShipment shopShipment : shopShipments){
            try {
                AddressGps addressGps = addressGpsCacher.findByBusinessIdAndType(shopShipment.getShopId(),AddressBusinessType.SHOP.getValue());
                distanceDtos.add(dispatchComponent.getDistance(addressGps,location.getLon(),location.getLat()));
            }catch (Exception e){
                log.error("find address gps by business id:{} and type:{} fail,cause:{}",shopShipment.getShopId(),AddressBusinessType.SHOP.getValue(), Throwables.getStackTraceAsString(e));
                throw new ServiceException("address.gps.not.found");
            }
        }

        //增序
        List<DistanceDto> sortDistance= dispatchComponent.sortDistanceDto(distanceDtos);

        Map<Long, ShopShipment> shopShipmentMap = shopShipments.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(ShopShipment::getShopId, it -> it));


        return shopShipmentMap.get(sortDistance.get(0).getId());

    }


    /**
     * 获取距离用户收货地址最近的仓
     *
     * @param shopShipments 店仓集合
     * @param address       用户收货地址
     * @return 距离最近的发货仓
     */
    public ShopShipment nearestShop(Map<Integer, List<Long>> priorityShopMap, List<ShopShipment> shopShipments, String address, String addressRegion) {
        for (Map.Entry<Integer, List<Long>> entry : priorityShopMap.entrySet()) {
            List<Long> priorityWarehouseIds = entry.getValue();
            List<ShopShipment> avail = Lists.newArrayList();
            if (!CollectionUtils.isEmpty(priorityWarehouseIds)) {
                for (Long id : priorityWarehouseIds) {
                    for (ShopShipment shopShipment : shopShipments) {
                        if (shopShipment.getShopId().equals(id)) {
                            avail.add(shopShipment);
                            shopShipments.remove(shopShipment);
                            break;
                        }
                    }
                }
            }
            if (avail.size() <= 0) {
                continue;
            } else if (avail.size() == 1) {
                return avail.get(0);
            } else {
                return getNearest(avail, address, addressRegion);
            }
        }
        return getNearest(shopShipments, address, addressRegion);
    }







}
