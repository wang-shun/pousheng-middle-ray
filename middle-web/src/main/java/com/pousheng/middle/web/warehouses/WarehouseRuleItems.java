package com.pousheng.middle.web.warehouses;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.pousheng.middle.enums.WareHousePriorityType;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.enums.WarehouseType;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.model.WarehouseRuleItem;
import com.pousheng.middle.warehouse.model.WarehouseShopGroup;
import com.pousheng.middle.warehouse.service.*;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.shop.convert.WarehouseRuleItemConverter;
import com.pousheng.middle.web.utils.operationlog.OperationLogModule;
import com.pousheng.middle.web.utils.operationlog.OperationLogParam;
import com.pousheng.middle.web.utils.operationlog.OperationLogType;
import com.pousheng.middle.web.warehouses.component.WarehouseRuleComponent;
import com.pousheng.middle.web.warehouses.dto.WarehouseRuleItemDto;
import io.swagger.annotations.ApiOperation;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.BeanMapper;
import io.terminus.common.utils.Splitters;
import io.terminus.parana.common.exception.InvalidException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-20
 */
@RestController
@RequestMapping("/api/warehouse/rule/{ruleId}/rule-item")
@Slf4j
@OperationLogModule(OperationLogModule.Module.WAREHOUSE_RULE_ITEM)
public class WarehouseRuleItems {

    @RpcConsumer
    private WarehouseRuleItemReadService warehouseRuleItemReadService;

    @RpcConsumer
    private WarehouseShopGroupReadService warehouseShopGroupReadService;

    @RpcConsumer
    private WarehouseRuleReadService warehouseRuleReadService;

    @RpcConsumer
    private WarehouseRuleItemWriteService warehouseRuleItemWriteService;

    @Autowired
    private WarehouseCacher warehouseCacher;
    @Autowired
    private WarehouseRuleComponent warehouseRuleComponent;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private WarehouseReadService warehouseReadService;

    @ApiOperation("根据规则ID查找仓库规则")
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<WarehouseRuleItemDto> findByRuleId(@PathVariable Long ruleId) {
        Response<List<WarehouseRuleItem>> r = warehouseRuleItemReadService.findByRuleId(ruleId);
        if (!r.isSuccess()) {
            log.error("failed to find warehouse rule items for rule(id={}), error code:{}", ruleId, r.getError());
            throw new JsonResponseException(r.getError());
        }
        List<WarehouseRuleItem> ruleItems =  r.getResult();
        List<WarehouseRuleItemDto> result = Lists.newArrayListWithCapacity(ruleItems.size());
        for (WarehouseRuleItem ruleItem : ruleItems) {
            Long warehouseId = ruleItem.getWarehouseId();
            Warehouse warehouse = warehouseCacher.findById(warehouseId);
            WarehouseRuleItemDto ruleItemDto = new WarehouseRuleItemDto();
            BeanMapper.copy(ruleItem, ruleItemDto);
            ruleItemDto.setCompanyCode(warehouse.getCompanyCode());
            if (warehouse.getExtra() == null) {
                ruleItemDto.setOutCode("");
            } else {
                ruleItemDto.setOutCode(
                        warehouse.getExtra().get("outCode") == null ? "" : warehouse.getExtra().get("outCode"));
            }
            result.add(ruleItemDto);
        }
        return result;
    }

    @ApiOperation("添加派单规则仓库")
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("批量创建")
    public Boolean save(@PathVariable @OperationLogParam Long ruleId,
                        @RequestBody WarehouseRuleItem[] warehouseRuleItems) {
        ArrayList<WarehouseRuleItem> ruleItemArrayList = Lists.newArrayList(warehouseRuleItems);
        //判断店仓
        checkAllChannel(ruleId,ruleItemArrayList);
        Response<Boolean> r = warehouseRuleItemWriteService.batchCreate(ruleId, ruleItemArrayList);
        if (!r.isSuccess()) {
            log.error("failed to save rule(id={})'s warehouseRuleItems:{}, error code:{}",
                    ruleId, warehouseRuleItems,r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }


    /**
     *
     * @param ruleId    规则
     * @param priority    距离:1 手动:2
     * @param outCode   批量仓库外码
     * @return
     */
    @ApiOperation("根据仓库外码批量添加派单规则仓库")
    @RequestMapping(value = "/batch", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean batchCreate(@PathVariable Long ruleId,
                               @RequestParam Integer priority,
                               @RequestParam String outCode) {
        ArrayList<String> outCodeList = Lists.newArrayList(Splitters.COMMA.splitToList(outCode));
        log.info("ware house out code list:{}", outCodeList.toString());
        // 校验仓库外码是否存在
        Response<List<String>> response = warehouseReadService.findByOutCode(outCodeList);
        if (!response.isSuccess()) {
            log.error("failed to find warehouse from out code:{}, error code:{}",
                    outCode, response.getError());
            throw new JsonResponseException(response.getError());
        }
        if (CollectionUtils.isNotEmpty(response.getResult())) {
            throw new InvalidException(500, "warehouse.outCode.find.fail(outCode={0})",
                    response.getResult().toString());
        }
        // 校验仓库范围
        Response<List<Warehouse>> listResponse = warehouseReadService.findWarehouseListByOutCode(outCodeList);
        if (!listResponse.isSuccess()) {
            log.error("failed to find warehouse from out code:{}, error code:{}",
                    outCode, response.getError());
            throw new JsonResponseException(response.getError());
        }
        List<WarehouseRuleItem> warehouseRuleItemList =
                WarehouseRuleItemConverter.convertToWarehouseRuleItemList(ruleId, listResponse.getResult());
        // 校验是否已经存在添加仓库
        Response<List<WarehouseRuleItem>> warehouseResp = warehouseRuleItemReadService.findByRuleId(ruleId);
        if (!warehouseResp.isSuccess()) {
            log.error("find warehouseRuleItem by ruleId :{} failed", ruleId);
            throw new JsonResponseException(response.getError());
        }
        // 已经存在的仓库id 不能继续添加
        List<WarehouseRuleItem> warehouseRuleItems = warehouseResp.getResult();
        List<Long> wareIdList = WarehouseRuleItemConverter.convertToExistWareHouse(
                warehouseRuleItems, warehouseRuleItemList);
        if (CollectionUtils.isNotEmpty(wareIdList)) {
            Response<List<Warehouse>> warehouseResponse = warehouseReadService.findByIds(wareIdList);
            if (!warehouseResponse.isSuccess()) {
                log.error("find Warehouse by id list :{} failed", wareIdList.toArray());
                throw new JsonResponseException(response.getError());
            }
            List<String> codeList =
                    WarehouseRuleItemConverter.convertToWareHouseCodeList(warehouseResponse.getResult());
            if (CollectionUtils.isNotEmpty(codeList)) {
                throw new InvalidException(500, "warehouse.out.code.exist(outCode={0})",
                        codeList.toString());
            }
        }
        // 距离优先 需要有详细地址信息
        if (priority == WareHousePriorityType.DISTANCE_PRIORITY_TYPE.getIndex()) {
            // 校验地址是否存在,按照距离添加的情况仓库必须要有具体地址
            List<String> addressList =
                    WarehouseRuleItemConverter.convertToWareHouseNullAddress(listResponse.getResult());
            if (CollectionUtils.isNotEmpty(addressList)) {
                throw new InvalidException(500, "warehouse.address.not.exist(outCode={0})",
                        addressList.toString());
            }
        }
        // 非全渠道店铺发货仓规则不能配置店仓
        List<String> qList = WarehouseRuleItemConverter.convertToWareHouseNotContainShop(listResponse.getResult());
        if (CollectionUtils.isNotEmpty(qList)) {
            throw new InvalidException(500, "can.not.contain.shop.warehouse(outCode={0})",
                    qList.toString());
        }
        warehouseRuleItems.addAll(warehouseRuleItemList);
        Response<Boolean> r = warehouseRuleItemWriteService.batchCreate(ruleId, warehouseRuleItems);
        if (!r.isSuccess()) {
            log.error("failed to save rule(id={})'s warehouseRuleItems:{}, error code:{}",
                    ruleId, warehouseRuleItemList,r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }



    private void checkAllChannel(Long ruleId,ArrayList<WarehouseRuleItem> ruleItemArrayList) {
        List<WarehouseShopGroup> shopGroups = warehouseRuleComponent.findWarehouseShopGropsByRuleId(ruleId);
        WarehouseShopGroup warehouseShopGroup = shopGroups.get(0);
        Boolean isAllChannelShop = orderReadLogic.isAllChannelOpenShop(warehouseShopGroup.getShopId());
        //是全渠道店铺直接返回，不用判断选择的仓范围是否包含店仓
        if (isAllChannelShop) {
            return;
        }
        //非全渠道则要判断仓范围中不能含有店仓
        for (WarehouseRuleItem warehouseRuleItem : ruleItemArrayList) {
            Warehouse warehouse = warehouseCacher.findById(warehouseRuleItem.getWarehouseId());
            if (Objects.equal(WarehouseType.from(warehouse.getType()),WarehouseType.SHOP_WAREHOUSE)) {
                log.error("warehouse(id:{}) is shop warehouse so can not add rule",warehouseRuleItem.getWarehouseId());
                throw new JsonResponseException("can.not.contain.shop.warehouse");
            }
        }

    }
}
