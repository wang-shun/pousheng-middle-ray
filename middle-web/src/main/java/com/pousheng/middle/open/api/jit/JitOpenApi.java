package com.pousheng.middle.open.api.jit;

import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.constants.SymbolConsts;
import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.mq.constant.MqConstants;
import com.pousheng.middle.open.api.OpenClientOrderApi;
import com.pousheng.middle.open.component.OpenOrderConverter;
import com.pousheng.middle.open.manager.JitOrderManager;
import com.pousheng.middle.order.dto.fsm.MiddleOrderType;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.web.biz.Exception.JitUnlockStockTimeoutException;
import com.pousheng.middle.web.utils.ApiParamUtil;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.open.client.order.dto.*;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.pampas.openplatform.entity.OPResponse;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import io.terminus.parana.common.constants.JitConsts;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 提供给JIT调用的接口
 *
 * @author tanlongjun
 */
@OpenBean
@Slf4j
public class JitOpenApi {

    @Autowired
    private EventBus eventBus;

    @Autowired
    private JitOrderManager jitOrderManager;

    @RpcConsumer
    private MiddleOrderReadService middleOrderReadService;
    @RpcConsumer
    private OpenShopReadService openShopReadService;

    @Autowired
    private WarehouseCacher warehouseCacher;

    @Autowired
    private OpenShopCacher openShopCacher;
    @Autowired
    private OpenOrderConverter openOrderConverter;

    @Autowired
    private OpenClientOrderApi openClientOrderApi;

    @Autowired
    private CompensateBizLogic compensateBizLogic;
    /**
     * 保存时效的订单
     *
     * @param orderInfo
     * @return
     */
    @OpenMethod(key = "push.out.rt.order.api", paramNames = {"orderInfo"}, httpMethods = RequestMethod.POST)
    public void saveRealTimeOrder(@NotNull(message = "order.info.is.null") String orderInfo) {
        log.info("PUSH-OUT-RT-ORDER-API START orderInfo:{}", orderInfo);
        Stopwatch stopwatch = Stopwatch.createStarted();

        OpenFullOrderInfo fullOrderInfo = validateBaiscParam(orderInfo);
        //参数验证
        validateBaiscParam(fullOrderInfo);
        OPResponse<String> response =null;
        try {
            ApiParamUtil.validateRequired(fullOrderInfo.getOrder(),"outOrderId","buyerName","companyCode","shopCode","fee","originFee","shipFee",
                "originShipFee","shipmentType","payType","status","channel","createdAt","stockId");
            for(OpenFullOrderItem item:fullOrderInfo.getItem()){
                ApiParamUtil.validateRequired(item,"outSkuorderId","skuCode","itemType","quantity","originFee","discount","cleanPrice","cleanFee");
            }
            ApiParamUtil.validateRequired(fullOrderInfo.getAddress(),"receiveUserName","province","city","region","detail");

            response = handleRealTimeOrder(fullOrderInfo);

        } catch (JitUnlockStockTimeoutException juste) {
            log.warn("lock stock timeout. try to save unlock task biz to recover stock.", orderInfo,
                Throwables.getStackTraceAsString(juste));
            jitOrderManager.saveUnlockInventoryTask(juste.getData());
        } catch (ServiceException se) {
            log.error("failed to save jit rt order.param:{},error:{}", orderInfo,
                se.getMessage());
            throw new OPServerException(200, se.getMessage());
        } catch (OPServerException oe) {
            log.error("failed to save jit rt order.param:{},error:{}", orderInfo,
                oe.getMessage());
            throw oe;
        } catch (Exception e) {
            log.error("failed to save jit realtime order.param:{},cause:{}", orderInfo,
                Throwables.getStackTraceAsString(e));
            throw new OPServerException(200, "failed.save.jit.realtime.order");
        }
        if (response == null
            || !response.isSuccess()) {
            log.error("failed to save jit realtime order.param:{},error:{}", orderInfo,
                response.getError());
            throw new OPServerException(200, response.getError());
        }
        stopwatch.stop();
        log.info("PUSH-OUT-RT-ORDER-API END. success.{} cost {} ms",orderInfo,stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    /**
     * 保存推送的订单
     *
     * @param orderInfo
     * @return
     */
    @OpenMethod(key = "push.out.jit.order.api", paramNames = {"orderInfo"}, httpMethods = RequestMethod.POST)
    public void saveOrder(@NotNull(message = "order.info.is.null") String orderInfo) {
        log.info("PUSH-OUT-JIT-ORDER-API START.orderInfo: {}", orderInfo);
        Stopwatch stopwatch = Stopwatch.createStarted();

        try {
            OpenFullOrderInfo fullOrderInfo = validateBaiscParam(orderInfo);
            //参数验证
             validateBaiscParam(fullOrderInfo);
             ApiParamUtil.validateRequired(fullOrderInfo.getOrder(),"outOrderId","buyerName","companyCode","shopCode","fee","originFee","shipFee",
                 "originShipFee","shipmentType","payType","type","status","channel","createdAt","stockId","interStockCode","interStockName","freightcompany",
                 "preFinishBillo","batchNo","batchMark","channelCode","expectDate","transportMethodCode","transportMethodName","cardRemark","jitOrderId");
             for(OpenFullOrderItem item:fullOrderInfo.getItem()){
                 ApiParamUtil.validateRequired(item,"outSkuorderId","vipsOrderId","skuCode","itemType","quantity","originFee","discount","cleanPrice","cleanFee");
             }

            ApiParamUtil.validateRequired(fullOrderInfo.getAddress(),"receiveUserName","province","city","region","detail","phone");

            if (StringUtils.isNotBlank(fullOrderInfo.getOrder().getRealtimeOrderIds())) {
                //验证时效订单是否存在
                Response<List<Long>> realOrderValidateResp = validateRealOrderIdsExist(fullOrderInfo);
                if (!realOrderValidateResp.isSuccess()) {
                    log.error("valid jit order info:{} fail,error:{}", orderInfo, realOrderValidateResp.getError());
                    throw new OPServerException(200, realOrderValidateResp.getError());
                }
            }
            //save to db
            Response<Long> response = saveDataToTask(orderInfo);
            if (!response.isSuccess()) {
                log.error("failed save jit big order:{}",orderInfo);
                throw new OPServerException("failed.save.jit.big.order");
            }

        } catch (ServiceException se) {
            log.error("failed to save jit order.param:{},error:{}", orderInfo,
                se.getMessage());
            throw new OPServerException(200, se.getMessage());
        } catch (OPServerException oe) {
            log.error("failed to save jit order.param:{},error:{}", orderInfo,
                oe.getMessage());
            throw oe;
        } catch (Exception e) {
            log.error("failed to save jit order.param:{},cause:{}", orderInfo,
                Throwables.getStackTraceAsString(e));
            throw new OPServerException(200, "failed.save.jit.big.order");
        }

        stopwatch.stop();
        log.info("PUSH-OUT-JIT-ORDER-API END.orderInfo: {}  cost {} ms", orderInfo,stopwatch.elapsed(TimeUnit.MILLISECONDS));


    }

    /**
     * 保存数据到补偿任务表
     *
     * @param data
     * @return
     */
    protected Response<Long> saveDataToTask(String data) {
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.OUT_OPEN_ORDER.toString());
        biz.setContext(data);
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        return Response.ok(compensateBizLogic.createBizAndSendMq(biz,MqConstants.POSHENG_MIDDLE_COMMON_COMPENSATE_BIZ_TOPIC));
    }

    protected OpenFullOrderInfo validateBaiscParam(String orderInfo) {
        if (StringUtils.isBlank(orderInfo)) {
            log.error("yj rt order info invalid is blank");
            throw new OPServerException(200,"orderInfo is required");
        }

        try {
            return JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(orderInfo, OpenFullOrderInfo.class);
        } catch (Exception e){
            log.error("trans order info json：{} to object fail,cause:{}",orderInfo, Throwables.getStackTraceAsString(e));
            throw new OPServerException("order.info.invalid");
        }
    }

    /**
     * 业务参数验证
     *
     * @param fullOrderInfo
     * @return
     */
    protected void validateBaiscParam(OpenFullOrderInfo fullOrderInfo) {
        if (fullOrderInfo == null) {
            throw new OPServerException(200,"param orderInfo incorrect");
        }
        OpenFullOrder openFullOrder = fullOrderInfo.getOrder();
        if (Objects.isNull(openFullOrder)) {
            throw new OPServerException(200,"openFullOrder.is.null");
        }
        List<OpenFullOrderItem> items = fullOrderInfo.getItem();
        if (Objects.isNull(items) || items.isEmpty()) {
            throw new OPServerException(200,"openFullOrderItems.is.null");
        }
        OpenFullOrderAddress address = fullOrderInfo.getAddress();
        if (Objects.isNull(address)) {
            throw new OPServerException(200,"openFullOrderAddress.is.null");
        }

    }

    /**
     * 验证时效订单是否存在
     *
     * @param fullOrderInfo
     * @return
     */
    protected Response<List<Long>> validateRealOrderIdsExist(OpenFullOrderInfo fullOrderInfo) {
        if (StringUtils.isBlank(fullOrderInfo.getOrder().getRealtimeOrderIds())) {
            return Response.ok();
        }
        List<String> outIds = Splitter.on(SymbolConsts.COMMA).trimResults().
            splitToList(fullOrderInfo.getOrder().getRealtimeOrderIds());
        if (CollectionUtils.isEmpty(outIds)){
            return Response.fail("realtimeOrderIds is required");
        }
        Response<List<ShopOrder>> response = middleOrderReadService.findByOutIdsAndOutFrom(
            outIds, JitConsts.YUNJU_REALTIME);
        if (response == null
            || !response.isSuccess()) {
            return Response.fail("failed.to.validate.realtime.orders");
        }
        if (CollectionUtils.isEmpty(response.getResult())) {
            return Response.fail("realtimeOrderIds.not.exist");
        }
        List<Long> orderIds = response.getResult().stream().map(ShopOrder::getId).collect(Collectors.toList());
        return Response.ok(orderIds);
    }

    /**
     * 处理时效订单
     * @param openFullOrderInfo
     * @return
     */
    protected OPResponse<String> handleRealTimeOrder(OpenFullOrderInfo openFullOrderInfo) {
        String shopCode = openFullOrderInfo.getOrder().getCompanyCode() + SymbolConsts.MINUS +
            openFullOrderInfo.getOrder()
                .getShopCode();

        //查询该渠道的店铺信息
        Long openShopId = openClientOrderApi.validateOpenShop(shopCode);
        OpenShop openShop = openShopCacher.findById(openShopId);
        Map<String, Integer> skuItemMap = Maps.newHashMap();
        for (OpenFullOrderItem item : openFullOrderInfo.getItem()) {
            skuItemMap.put(item.getSkuCode(), item.getQuantity());
        }

        //查询仓库编号
        String stockId = openFullOrderInfo.getOrder().getStockId();
        WarehouseDTO warehouseDTO = warehouseCacher.findByCode(stockId);

        //验证库存是否足够
        boolean outstock = jitOrderManager.validateInventory(openShopId, warehouseDTO.getId(), skuItemMap);
        if (!outstock) {
            return OPResponse.fail("inventory.not.enough");
        }

        //业务参数校验
        OPResponse<String> response = jitOrderManager.validateBusiParam(openFullOrderInfo);
        if (!response.isSuccess()) {
            return response;
        }
        //组装参数
        OpenClientFullOrder openClientFullOrder = openOrderConverter.transform(openFullOrderInfo, openShop);

        //设置为jit时效订单类型
        openClientFullOrder.setType(MiddleOrderType.JIT_REAL_TIME.getValue());
        //保存中台仓库id
        Map<String,String> extraMap=openClientFullOrder.getExtra();
        extraMap.put(JitConsts.WAREHOUSE_ID,String.valueOf(warehouseDTO.getId()));
        //保存订单
        jitOrderManager.handleReceiveOrder(OpenClientShop.from(openShop), Lists.newArrayList(openClientFullOrder),
            warehouseDTO.getId());

        return OPResponse.ok();
    }
}
