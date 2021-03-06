package com.pousheng.middle.warehouse.companent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.dto.AvailableInventoryDTO;
import com.pousheng.middle.warehouse.dto.AvailableInventoryRequest;
import com.pousheng.middle.warehouse.dto.InventoryDTO;
import com.pousheng.middle.warehouse.dto.InventoryTradeDTO;
import com.pousheng.middle.warehouse.dto.ShopSkuSupplyRule;
import com.pousheng.middle.warehouse.dto.ShopSkuSupplyRuleBatchCreateRequest;
import com.pousheng.middle.warehouse.dto.ShopSkuSupplyRuleBatchUpdateDisableRequest;
import com.pousheng.middle.warehouse.dto.ShopSkuSupplyRuleCreateRequest;
import com.pousheng.middle.warehouse.dto.ShopSkuSupplyRuleQueryOneRequest;
import com.pousheng.middle.warehouse.dto.ShopSkuSupplyRulesDTO;
import com.pousheng.middle.warehouse.model.SkuInventory;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 库存操作相关接口
 *
 * @auther feisheng.ch
 * @time 2018/5/25
 */
@Component
@Slf4j
public class InventoryClient {

    @Autowired
    private InventoryBaseClient inventoryBaseClient;

    @Value("${inventory.domain.url}")
    private String domainUrl;

    public static final int HttpTime = 300000;

    /**
     * 锁定（对应库存中心的占用操作）
     * @return
     */
    public Response<Boolean> lock (List<InventoryTradeDTO> inventoryTradeDTOList) {
        return trade(inventoryTradeDTOList, "api/inventory/trade/occupy");
    }

    /**
     * 手工派单走的锁定接口，请慎用（对应库存中心的占用操作）
     * @return
     */
    public Response<Boolean> lockUserDispatch (List<InventoryTradeDTO> inventoryTradeDTOList) {
        return trade(inventoryTradeDTOList, "api/inventory/trade/occupyUserDispatch");
    }

    /**
     * 解锁（对应库存中心的解除占用操作）
     * @return
     */
    public Response<Boolean> unLock (List<InventoryTradeDTO> inventoryTradeDTOList) {
        return trade(inventoryTradeDTOList, "api/inventory/trade/unOccupy");
    }

    /**
     * 取消占用补偿接口：
     *  1. 如果没有占用事件，直接返回成功
     *  2. 如果已经取消过，返回成功
     *  3. 执行取消逻辑，返回结果
     * @param inventoryTradeDTOList
     * @return
     */
    public Response<Boolean> unLockForCompensate (List<InventoryTradeDTO> inventoryTradeDTOList) {
        return trade(inventoryTradeDTOList, "api/inventory/trade/unOccupyForCompensate");
    }

    /**
     * 扣减（对应库存中心的扣减操作）
     * @return
     */
    public Response<Boolean> decrease (List<InventoryTradeDTO> inventoryTradeDTOList) {
        return trade(inventoryTradeDTOList, "api/inventory/trade/decrease");
    }

    private Response<Boolean> trade (List<InventoryTradeDTO> inventoryTradeDTOList, String apiPath) {
        if (ObjectUtils.isEmpty(inventoryTradeDTOList)) {
            return Response.fail("inventory.trade.fail.parameter");
        }
        try {
            return Response.ok((Boolean) inventoryBaseClient.postJson(apiPath,
                    JSON.toJSONString(inventoryTradeDTOList), Boolean.class));
        } catch (Exception e) {
            log.error("fail to trade inventory, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }
    }


    public Response<SkuInventory> findInventoryById(Long id) {
        if (ObjectUtils.isEmpty(id)) {
            return Response.fail("inventory.fail.parameter");
        }
        try {
            return Response.ok((SkuInventory) inventoryBaseClient.post("api/inventory/query/" + id, ImmutableMap.of("id", id), SkuInventory.class));
        } catch (Exception e) {
            log.error("fail to trade inventory, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }
    }

    /**
     * 获取可用库存数量
     *
     * @param requests
     * @param shopId
     * @return
     */
    public Response<List<AvailableInventoryDTO>> getAvailableInventory (List<AvailableInventoryRequest> requests, Long shopId) {
        if (ObjectUtils.isEmpty(requests)) {
            return Response.fail("inventory.available.fail.parameter");
        }
        if (null == shopId) {
            shopId = -1L;
        }

        try {
            List<AvailableInventoryDTO> availableInvList = (List<AvailableInventoryDTO>)inventoryBaseClient.postJsonRetList("api/inventory/query/getAvailableInventory/"+shopId,
                    JSON.toJSONString(requests), AvailableInventoryDTO.class);
            //log.info("get available inventory shopId:{}  requestJson:{}  result:{}", shopId, JSON.toJSONString(requests), JSON.toJSONString(availableInvList));
            if (ObjectUtils.isEmpty(availableInvList)) {
                return Response.ok(Lists.newArrayList());
            }
            return Response.ok(availableInvList);
        } catch (Exception e) {
            log.error("get available inventory fail, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }
    }


    /**
     * 获取可用库存数量
     *
     * @param requests
     * @param shopId
     * @return
     */
    public Response<List<AvailableInventoryDTO>> getAvailableInventoryWithoutSupply(List<AvailableInventoryRequest> requests, Long shopId) {
        if (ObjectUtils.isEmpty(requests)) {
            return Response.fail("inventory.available.fail.parameter");
        }
        if (null == shopId) {
            shopId = -1L;
        }

        try {
            List<AvailableInventoryDTO> availableInvList = (List<AvailableInventoryDTO>) inventoryBaseClient.postJsonRetList("api/inventory/query/available/" + shopId,
                    JSON.toJSONString(requests), AvailableInventoryDTO.class);
            if (ObjectUtils.isEmpty(availableInvList)) {
                return Response.ok(Lists.newArrayList());
            }
            return Response.ok(availableInvList);
        } catch (Exception e) {
            log.error("get available inventory fail, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }
    }


    /**
     * 获取可用库存数量，忽略仓库，返回列表中也没有仓库ID信息
     * @param requests
     * @param shopId
     * @return
     */
    public Response<List<AvailableInventoryDTO>> getAvailInvRetNoWarehouse (List<AvailableInventoryRequest> requests, Long shopId) {
        if (ObjectUtils.isEmpty(requests)) {
            return Response.fail("inventory.available.fail.parameter");
        }

        if (null == shopId) {
            shopId = -1L;
        }

        String reqNo =  UUID.randomUUID().toString().replace("-","");
        try {


            log.info(" start to getAvailInvRetNoWarehouse (reqNo:{}) ",reqNo );
            //log.info(" start to getAvailInvRetNoWarehouse (param:{}) ",JSON.toJSONString(requests));
            List<AvailableInventoryDTO> availableInvList = (List<AvailableInventoryDTO>)inventoryBaseClient.postJsonRetList("api/inventory/query/getAvailInvRetNoWarehouse/"+shopId+"/"+reqNo ,
                    JSON.toJSONString(requests), AvailableInventoryDTO.class);
            log.info(" end to getAvailInvRetNoWarehouse (reqNo:{}) ",reqNo );
            if (ObjectUtils.isEmpty(availableInvList)) {
                return Response.ok(Lists.newArrayList());
            }

            return Response.ok(availableInvList);
        } catch (Exception e) {
            log.error("get available inventory fail,reqNo:{} cause:{}", reqNo,Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }
    }

    /**
     * 查询某个sku在指定仓库的库存情况
     *
     * @param warehouseId
     * @param skuCode
     * @return
     */
    public Response<InventoryDTO> findByWarehouseIdAndSkuCode (Long warehouseId, String skuCode) {
        if (null == warehouseId && ObjectUtils.isEmpty(skuCode)) {
            return Response.fail("inventory.find.fail.parameter");
        }
        try {
            Response<List<InventoryDTO>> inventoryRes = findSkuStocks(warehouseId, Lists.newArrayList(skuCode));
            if (inventoryRes.isSuccess() && ObjectUtils.isEmpty(inventoryRes.getResult())) {
                InventoryDTO inv = new InventoryDTO();
                inv.setWarehouseId(warehouseId);
                inv.setSkuCode(skuCode);
                inv.setRealQuantity(0L);
                inv.setPreorderQuantity(0L);
                inv.setWithholdQuantity(0L);
                inv.setOccupyQuantity(0L);
                inv.setSafeQuantity(0L);

                return Response.ok(inv);
            }

            return Response.ok(inventoryRes.getResult().get(0));
        } catch (Exception e) {
            log.error("get fail, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }
    }

    /**
     * 查找库存行
     * @param warehouseId
     * @param skuCode 区分 0 和 没有
     * @return
     */
    public Response<Optional<InventoryDTO>> findInventory (Long warehouseId, String skuCode) {
        if (null == warehouseId || ObjectUtils.isEmpty(skuCode)) {
            return Response.fail("inventory.find.fail.parameter");
        }
        try {
            Response<List<InventoryDTO>> inventoryRes = findSkuStocks(warehouseId, Lists.newArrayList(skuCode));
            if (inventoryRes.isSuccess() && ObjectUtils.isEmpty(inventoryRes.getResult())) {
                return Response.ok(Optional.empty());
            }

            return Response.ok(Optional.of(inventoryRes.getResult().get(0)));
        } catch (Exception e) {
            log.error("get fail, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }
    }

    /**
     * 获取多个sku在指定仓库下的库存情况
     *
     * @param warehouseId
     * @param skuCodeList
     * @return
     */
    public Response<List<InventoryDTO>> findSkuStocks (Long warehouseId, List<String> skuCodeList) {
        if (null == warehouseId && ObjectUtils.isEmpty(skuCodeList)) {
            return Response.fail("inventory.find.fail.parameter");
        }
        try {
            Map<String, Object> params = Maps.newHashMap();
            params.put("warehouseId", warehouseId);
            params.put("skuCodeList", (null==skuCodeList?JSON.toJSONString(Lists.newArrayList()):JSON.toJSONString(skuCodeList)));

            return Response.ok((List<InventoryDTO>) inventoryBaseClient.get(
                    "api/inventory/query/findInventoryByWarehouseSkus",
                    null, null, params, InventoryDTO.class, true));

        } catch (Exception e) {
            log.error("find all config shop id list fail, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }
    }

    /**
     * 库存分页
     *
     * @return
     */
    public Paging<SkuInventory> inventoryPaging (Integer pageNo, Integer pageSize, List<String> skuCodeList, String warehouseName, Integer realQuantity) {
        if (null == pageNo) {
            pageNo = 1;
        }
        if (null == pageSize) {
            pageSize = 20;
        }
        try {
            Map<String, Object> params = Maps.newHashMap();
            params.put("warehouseName", warehouseName);
            params.put("skuCodeList", (null==skuCodeList?JSON.toJSONString(Lists.newArrayList()):JSON.toJSONString(skuCodeList)));
            if (realQuantity != null) {
                params.put("realQuantity", realQuantity);
            }

            Paging<JSONObject> dataPage = (Paging<JSONObject>) inventoryBaseClient.get(
                    "api/inventory/query/paging",
                    pageNo, pageSize, params, Paging.class, false);

            if (null != dataPage) {
                List<SkuInventory> temp = Lists.newArrayList();
                for (JSONObject jsonObject : dataPage.getData()) {
                    temp.add(JSON.parseObject(JSON.toJSONString(jsonObject), SkuInventory.class));
                }

                return new Paging<SkuInventory>(dataPage.getTotal(), temp);
            }

            return Paging.empty();

        } catch (Exception e) {
            log.error("page inventory fail, cause:{}", Throwables.getStackTraceAsString(e));

            return Paging.empty();
        }
    }

    /**
     * 获取指定sku的库存列表
     * @param skuCode
     * @return
     */
    public Response<List<InventoryDTO>> findBySkuCode (String skuCode) {
        return findSkuStocks(null, Lists.newArrayList(skuCode));
    }


    /**
     * 获取多个sku在所有仓库下的可用库存情况
     *
     * @param skuCodeList
     * @return
     */
    public Response<List<AvailableInventoryDTO>> getAvailableNoNeedWarehouseAndShop (List<String> skuCodeList) {
        try {
            List<AvailableInventoryDTO> availableInvList = (List<AvailableInventoryDTO>)inventoryBaseClient.postJsonRetList("api/inventory/query/getAvailableNoNeedWarehouseAndShop",
                    JSON.toJSONString(skuCodeList), AvailableInventoryDTO.class);
            if (ObjectUtils.isEmpty(availableInvList)) {
                return Response.ok(Lists.newArrayList());
            }
            return Response.ok(availableInvList);
        } catch (Exception e) {
            log.error("get available inventory fail, cause:{}",Throwables.getStackTraceAsString(e));
            return Response.fail(e.getMessage());
        }
    }

    /**
     * 保存商品供货规则
     */
    public Response<Boolean> saveShopSkuSupplyRule(ShopSkuSupplyRuleCreateRequest request) {
        try {
            Boolean result = (Boolean) inventoryBaseClient.postJson("api/inventory/shop/sku/supply/rule/create", JsonMapper.nonEmptyMapper().toJson(request), Boolean.class);
            return Response.ok(result);
        } catch (Exception e) {
            log.error("fail save shop sku supply rule by request:{}, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("save.sku.supply.rule.fail");
        }
    }

    /**
     * 保存商品供货规则 & 范围
     */
    public Response<Boolean> batchSaveShopSkuSupplyRule(ShopSkuSupplyRuleBatchCreateRequest request) {
        Boolean result = (Boolean) inventoryBaseClient.postJson("api/inventory/shop/sku/supply/rule/batch/create", JsonMapper.nonEmptyMapper().toJson(request), Boolean.class);
        return Response.ok(result);
    }

    /**
     * 根据店铺与 sku 查询一个发货限制
     */
    public Response<ShopSkuSupplyRule> queryByShopIdAndSkuCode(ShopSkuSupplyRuleQueryOneRequest request) {
        ShopSkuSupplyRule result = (ShopSkuSupplyRule) inventoryBaseClient.postJson("api/inventory/shop/sku/supply/rule/find", JsonMapper.nonEmptyMapper().toJson(request), ShopSkuSupplyRule.class);
        return Response.ok(result);
    }

    /**
     * 查询仓库所有sku的真实库存大于0的库存信息
     *
     * @param warehouseCode
     * @return
     */
    public Response<List<AvailableInventoryDTO>> queryAvailableRealQtyByWarehouse(String warehouseCode) {
        try {
            String path = "api/inventory/query/inventory/list/" + warehouseCode;
            List<AvailableInventoryDTO> availableInvList = (List<AvailableInventoryDTO>)inventoryBaseClient
                .postJsonRetList(path, "", AvailableInventoryDTO.class);
            if (ObjectUtils.isEmpty(availableInvList)) {
                return Response.ok(Lists.newArrayList());
            }
            return Response.ok(availableInvList);
        } catch (Exception e) {
            log.error("query available inventory fail, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail(e.getMessage());
        }
    }

    /**
     * 批量更新发货规则禁用状态
     * @param request
     * @return
     */
    public Response<Long> batchUpdateDisable(ShopSkuSupplyRuleBatchUpdateDisableRequest request) {

        try {
            String path = "api/inventory/shop/sku/supply/rule/batch/disable";
            Long result = (Long) inventoryBaseClient.postJson(path, JsonMapper.nonEmptyMapper().toJson(request), Long.class);
            return Response.ok(result);
        } catch (Exception e) {
            log.error("fail to batch update disable request:{}, cause:{}", request, e);
            return Response.fail(e.getMessage());
        }
    }

    /**
     * 获取发货规则最新id
     * @return
     */
    public Response<Long> queryTopSupplyRuleId() {
        try {
            String path = "api/inventory/shop/sku/supply/rule/topId";
            Long id = (Long) inventoryBaseClient.get(path, null, null, Maps.newHashMap(), Long.class, false);
            return Response.ok(id);
        } catch (Exception e) {
            log.error("query sku supply rule id fail, cause:{}", e);
            return Response.fail(e.getMessage());
        }
    }

	/**
     *  更新设置库存中心安全库存
     * @param id
     * @param safeQuantity
     * @return
     */
    public Response<String> setSkuInventorySafeStock(Long id, Integer safeQuantity){
        try {
            Map<String, Object> params = Maps.newHashMap();
            params.put("safeQuantity", safeQuantity);
            String path = "api/inventory/" +id+ "/safeQuantity/setting";
            HttpRequest r = HttpRequest.put(domainUrl+"/"+path, params, true)
                    .acceptJson()
                    .acceptCharset(HttpRequest.CHARSET_UTF8).connectTimeout(HttpTime).readTimeout(HttpTime);
            String body = r.body();
            if (r.ok()){
                log.info("request to inventory api {} with params: {} success response body:{}", domainUrl + "/" + path, params,body);
                return Response.ok("");  
            } else{
                log.info("request to inventory api {} with params: {} fail response body:{}", domainUrl + "/" + path, params,body);
                return Response.fail("request.inventory.fail");
            }
        } catch (Exception e) {
            log.error("set inventory safeStocks failed,inventoryId:{}, cause:{}",id, Throwables.getStackTraceAsString(e)); 
            return Response.fail(e.getMessage());
        }
    }


    /**
     * 根据店铺与 sku 查询一个发货限制明细
     */
    public Response<ShopSkuSupplyRulesDTO> queryFullShopSkuSupplyRule(ShopSkuSupplyRuleQueryOneRequest request) {
        Object o = inventoryBaseClient.postJson("api/inventory/shop/sku/supply/rule/find/full", JsonMapper.nonEmptyMapper().toJson(request), ShopSkuSupplyRulesDTO.class);
        if (o == null) {
            return Response.ok();
        }
        return Response.ok((ShopSkuSupplyRulesDTO)o);
    }
}
