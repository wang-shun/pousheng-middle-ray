package com.pousheng.middle.web.warehouses.component;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.hksyc.dto.trade.ReceiverInfoHandleResult;
import com.pousheng.middle.order.model.AddressGps;
import com.pousheng.middle.warehouse.cache.WarehouseAddressCacher;
import com.pousheng.middle.warehouse.model.WarehouseAddress;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 仓库地址转换器
 * Created by songrenfei
 */
@Component
@Slf4j
public class WarehouseAddressTransverter {

    @Autowired
    private WarehouseAddressCacher warehouseAddressCacher;

    //转换仓库地址信息
    public void complete(AddressGps addressGps){
        ReceiverInfoHandleResult handleResult = new ReceiverInfoHandleResult();
        handleResult.setSuccess(Boolean.TRUE);
        List<String> errors = Lists.newArrayList();

        //目前中台省的pid都是1，所以这里直接写死，如果有变动的话这里也需要做对应的修改
        //移除省市区转换 removed by longjun.tlj
//        Long provinceId = queryAddressId(1L,addressGps.getProvince());
//        if(Arguments.notNull(provinceId)){
//            addressGps.setProvinceId(provinceId);
//        }else {
//            handleResult.setSuccess(Boolean.FALSE);
//            errors.add("恒康仓库地址省："+addressGps.getProvince()+"未匹配到中台的省");
//        }
//
//        Long cityId = queryAddressId(provinceId,addressGps.getCity());
//        if(Arguments.notNull(cityId)){
//            addressGps.setCityId(cityId);
//        }else {
//            handleResult.setSuccess(Boolean.FALSE);
//            errors.add("恒康仓库地址市："+addressGps.getCity()+"未匹配到中台的市");
//        }
//
//        if (StringUtils.hasText(addressGps.getRegion())){
//            Long regionId = queryAddressId(cityId,addressGps.getRegion());
//            if(Arguments.notNull(regionId)){
//                addressGps.setRegionId(regionId);
//            }else {
//                handleResult.setSuccess(Boolean.FALSE);
//                errors.add("恒康仓库地址区："+addressGps.getRegion()+"未匹配到中台的区");
//            }
//        }
//
//        handleResult.setErrors(errors);
        Map<String,String> extraMap = Maps.newHashMap();
        extraMap.put("handleResult", JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(handleResult));
        addressGps.setExtra(extraMap);
    }

    private Long queryAddressId(Long pid,String name){
        //pid为null则直接返回null
        if(Arguments.isNull(pid)){
            return null;
        }
        Optional<WarehouseAddress> wo1 = warehouseAddressCacher.findByPidAndName(pid,name);
        if(wo1.isPresent()){
            return wo1.get().getId();
        }

        String splitName = name.substring(0,2);
        Optional<WarehouseAddress> wo2 = warehouseAddressCacher.findByPidAndName(pid,splitName);
        if(wo2.isPresent()){
            return wo2.get().getId();
        }

        return null;
    }


}
