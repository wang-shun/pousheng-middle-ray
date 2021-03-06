package com.pousheng.middle.web.order.component;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.*;
import com.pousheng.middle.open.api.dto.YYEdiRefundConfirmItem;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.*;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.enums.RefundSource;
import com.pousheng.middle.order.model.RefundWarehouseRules;
import com.pousheng.middle.order.service.MiddleRefundReadService;
import com.pousheng.middle.order.service.RefundWarehouseRulesReadService;
import com.pousheng.middle.warehouse.companent.WarehouseClient;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.web.utils.SkuCodeUtil;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.model.ShipmentItem;
import io.terminus.parana.order.service.RefundReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mail: F@terminus.io
 * Data: 16/7/13
 * Author: yangzefeng
 */
@Component
@Slf4j
public class RefundReadLogic {

    @Autowired
    private MiddleRefundReadService middleRefundReadService;
    @RpcConsumer
    private RefundReadService refundReadService;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private WarehouseClient warehouseClient;
    @Autowired
    private RefundWarehouseRulesReadService refundWarehouseRulesReadService;

    @Autowired
    private MiddleOrderFlowPicker flowPicker;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();


    public Response<Paging<RefundPaging>> refundPaging(MiddleRefundCriteria criteria) {
        Response<Paging<Refund>> refundsR = middleRefundReadService.paging(criteria);
        if (!refundsR.isSuccess()) {
            log.error("paging refund by criteria:{} fail,error:{}", criteria, refundsR.getError());
            return Response.fail(refundsR.getError());
        }

        Paging<Refund> refundPaging = refundsR.getResult();
        List<Refund> refunds = refundPaging.getData();
        Response<Map<Long, OrderRefund>> groupByRefundIdMapRes = groupOrderRerundByRefundId(refunds);
        if (!groupByRefundIdMapRes.isSuccess()) {
            return Response.fail(groupByRefundIdMapRes.getError());
        }

        Map<Long, OrderRefund> groupByRefundIdMap = groupByRefundIdMapRes.getResult();
        Flow flow = flowPicker.pickAfterSales();
        Paging<RefundPaging> paging = new Paging<>();
        paging.setTotal(refundPaging.getTotal());
        List<RefundPaging> refundLists = Lists.newArrayListWithCapacity(refunds.size());

        for (Refund refund : refunds) {
            RefundPaging rp = new RefundPaging();
            rp.setRefund(refund);
            rp.setOrderRefund(groupByRefundIdMap.get(refund.getId()));
            Set<OrderOperation> operations = flow.availableOperations(refund.getStatus());
            rp.setOperations(operations);
            refundLists.add(rp);
        }
        paging.setData(refundLists);
        return Response.ok(paging);
    }

    public Response<Map<Long, OrderRefund>> groupOrderRerundByRefundId(List<Refund> refunds) {

        if (CollectionUtils.isEmpty(refunds)) {
            return Response.ok(Maps.newHashMap());
        }
        List<Long> refundIds = Lists.transform(refunds, new Function<Refund, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable Refund refund) {
                return refund.getId();
            }
        });
        Response<List<OrderRefund>> listRes = middleRefundReadService.findOrderRefundByRefundIds(refundIds);
        if (!listRes.isSuccess()) {
            log.error("find order refund by refund ids:{} fail,error:{}", refundIds, listRes.getError());
            return Response.fail(listRes.getError());
        }
        List<OrderRefund> orderRefunds = listRes.getResult();
        Map<Long, OrderRefund> groupByRefundIdMap = orderRefunds.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(OrderRefund::getRefundId, it -> it));
        return Response.ok(groupByRefundIdMap);
    }

    public Refund findRefundById(Long refundId) {
        Response<Refund> refundRes = refundReadService.findById(refundId);
        if (!refundRes.isSuccess()) {
            log.error("find refund by id:{} fail,error:{}", refundId, refundRes.getError());
            throw new JsonResponseException(refundRes.getError());
        }

        return refundRes.getResult();
    }

    public Refund findRefundByOutId(String refundOutId) {
        Response<Refund> refundRes = refundReadService.findByOutId(refundOutId);
        if (!refundRes.isSuccess()) {
            log.error("find refund by out id:{} fail,error:{}", refundOutId, refundRes.getError());

            throw new JsonResponseException(refundRes.getError());
        }

        return refundRes.getResult();
    }

    public Refund findRefundByRefundCode(String refundCode) {
        Response<Refund> refundRes = refundReadService.findByRefundCode(refundCode);
        if (!refundRes.isSuccess()) {
            log.error("find refund by refundCode:{} fail,error:{}", refundCode, refundRes.getError());
            throw new JsonResponseException(refundRes.getError());
        }

        return refundRes.getResult();
    }


    public List<Refund> findRefundByIds(List<Long> refundIds) {
        Response<List<Refund>> refundRes = refundReadService.findByIds(refundIds);
        if (!refundRes.isSuccess()) {
            log.error("find refund by ids:{} fail,error:{}", refundIds, refundRes.getError());
            throw new JsonResponseException(refundRes.getError());
        }

        return refundRes.getResult();
    }


    public OrderRefund findOrderRefundByRefundId(Long refundId) {
        Response<List<OrderRefund>> listRes = refundReadService.findOrderIdsByRefundId(refundId);
        if (!listRes.isSuccess()) {
            log.error("find order refund by refund id:{} fail,error:{}", refundId, listRes.getError());
            throw new JsonResponseException(listRes.getError());
        }

        List<OrderRefund> orderRefunds = listRes.getResult();
        if (CollectionUtils.isEmpty(orderRefunds)) {
            log.error("not find order refund by refund id:{}", refundId);
            throw new JsonResponseException("order.refund.not.exist");
        }

        return orderRefunds.get(0);

    }

    public List<OrderRefund> findOrderRefundsByOrderId(Long shopOrderId) {
        Response<List<OrderRefund>> response = middleRefundReadService.findOrderRefundsByOrderId(shopOrderId);
        if (!response.isSuccess()) {
            log.error("find order refunds by orderId ids:{} fail,cause:{}", shopOrderId, response.getError());
            throw new JsonResponseException("order.refund.not.exist");
        }
        return response.getResult();
    }

    public List<Refund> findRefundsByOrderId(Long shopOrderId) {
        Response<List<Refund>> response = refundReadService.findByOrderIdAndOrderLevel(shopOrderId, OrderLevel.SHOP);
        if (!response.isSuccess()) {
            log.error("find  refunds by orderId ids:{} fail,cause:{}", shopOrderId, response.getError());
            throw new JsonResponseException("refund.not.exist");
        }
        return response.getResult();
    }

    public List<Refund> findRefundsByOrderCode(String shopOrderCode) {
        Response<List<Refund>> response = refundReadService.findByOrderCodeAndOrderLevel(shopOrderCode, OrderLevel.SHOP);
        if (!response.isSuccess()) {
            log.error("find  refunds by shopOrderCode ids:{} fail,cause:{}", shopOrderCode, response.getError());
            throw new JsonResponseException("refund.not.exist");
        }
        return response.getResult();
    }

    public List<RefundItem> findRefundItems(Refund refund) {
        Map<String, String> extraMap = refund.getExtra();
        if (CollectionUtils.isEmpty(extraMap)) {
            log.warn("refund(id:{}) extra field is null", refund.getId());
            return Lists.newArrayList();
        }
        if (!extraMap.containsKey(TradeConstants.REFUND_ITEM_INFO)) {
            log.warn("refund(id:{}) extra map not contain key:{}", refund.getId(), TradeConstants.REFUND_ITEM_INFO);
            return Lists.newArrayList();
        }
        return mapper.fromJson(extraMap.get(TradeConstants.REFUND_ITEM_INFO), mapper.createCollectionType(List.class, RefundItem.class));
    }

    public List<YYEdiRefundConfirmItem> findRefundYYEdiConfirmItems(Refund refund) {
        Map<String, String> extraMap = refund.getExtra();
        if (CollectionUtils.isEmpty(extraMap)) {
            log.warn("refund(id:{}) extra field is null", refund.getId());
            return Lists.newArrayList();
        }
        if (!extraMap.containsKey(TradeConstants.REFUND_YYEDI_RECEIVED_ITEM_INFO)) {
            log.warn("refund(id:{}) extra map not contain key:{}", refund.getId(), TradeConstants.REFUND_YYEDI_RECEIVED_ITEM_INFO);
            return Lists.newArrayList();
        }
        return mapper.fromJson(extraMap.get(TradeConstants.REFUND_YYEDI_RECEIVED_ITEM_INFO), mapper.createCollectionType(List.class, YYEdiRefundConfirmItem.class));
    }


    public List<RefundItem> findRefundChangeItems(Refund refund) {
        Map<String, String> extraMap = refund.getExtra();
        if (CollectionUtils.isEmpty(extraMap)) {
            log.error("refund(id:{}) extra field is null", refund.getId());
            throw new JsonResponseException("refund.extra.is.empty");
        }
        if (!extraMap.containsKey(TradeConstants.REFUND_CHANGE_ITEM_INFO)) {
            log.error("refund(id:{}) extra map not contain key:{}", refund.getId(), TradeConstants.REFUND_CHANGE_ITEM_INFO);
            throw new JsonResponseException("refund.exit.not.contain.item.info");
        }
        return mapper.fromJson(extraMap.get(TradeConstants.REFUND_CHANGE_ITEM_INFO), mapper.createCollectionType(List.class, RefundItem.class));
    }


    public RefundSource findRefundSource(Refund refund) {
        Map<String, String> tagMap = refund.getTags();
        if (CollectionUtils.isEmpty(tagMap)) {
            log.error("refund(id:{}) tag_Json field is null", refund.getId());
            throw new JsonResponseException("refund.tag.is.empty");
        }
        if (!tagMap.containsKey(TradeConstants.REFUND_SOURCE)) {
            log.error("refund(id:{}) extra map not contain key:{}", refund.getId(), TradeConstants.REFUND_SOURCE);
            throw new JsonResponseException("refund.exit.not.contain.item.info");
        }
        return RefundSource.from(Integer.valueOf(tagMap.get(TradeConstants.REFUND_SOURCE)));
    }


    public RefundExtra findRefundExtra(Refund refund) {
        Map<String, String> extraMap = refund.getExtra();
        if (CollectionUtils.isEmpty(extraMap)) {
            log.warn("refund(id:{}) extra field is null", refund.getId());
            return new RefundExtra();
        }
        if (!extraMap.containsKey(TradeConstants.REFUND_EXTRA_INFO)) {
            log.warn("refund(id:{}) extra map not contain key:{}", refund.getId(), TradeConstants.REFUND_EXTRA_INFO);
            return new RefundExtra();
        }

        return mapper.fromJson(extraMap.get(TradeConstants.REFUND_EXTRA_INFO), RefundExtra.class);
    }

    public MiddleChangeReceiveInfo findMiddleChangeReceiveInfo(Refund refund) {
        Map<String, String> extraMap = refund.getExtra();
        if (CollectionUtils.isEmpty(extraMap)) {
            log.warn("refund(id:{}) extra field is null", refund.getId());
            return new MiddleChangeReceiveInfo();
        }
        if (!extraMap.containsKey(TradeConstants.MIDDLE_CHANGE_RECEIVE_INFO)) {
            log.warn("refund(id:{}) extra map not contain key:{}", refund.getId(), TradeConstants.MIDDLE_CHANGE_RECEIVE_INFO);
            return new MiddleChangeReceiveInfo();
        }
        return mapper.fromJson(extraMap.get(TradeConstants.MIDDLE_CHANGE_RECEIVE_INFO), MiddleChangeReceiveInfo.class);
    }

    /**
     * 根据id查询规则
     * @param id
     * @return
     */
    public MiddleRefundWarehouseRuleDto findRefundWarehouseRuleDtoById(Long id) {
        MiddleRefundWarehouseRuleDto ruleDto = new MiddleRefundWarehouseRuleDto();
        Response<RefundWarehouseRules> refundruleRes = refundWarehouseRulesReadService.findRulesById(id);
        if (!refundruleRes.isSuccess()) {
            log.error("find refundWarehouseRules by id:{} fail,error:{}", id, refundruleRes.getError());
            throw new JsonResponseException(refundruleRes.getError());
        }
        //通过退货仓代码查询退货仓名称
        WarehouseDTO warehousedto = findWarehouseByCodeandCompanyId(refundruleRes.getResult().getRefundWarehouseCode());
        ruleDto.setId(refundruleRes.getResult().getId());
        ruleDto.setOrderShopCode(refundruleRes.getResult().getOrderShopCode());
        ruleDto.setShopId(refundruleRes.getResult().getShopId());
        ruleDto.setOrderShopName(refundruleRes.getResult().getOrderShopName());
        ruleDto.setShipmentCompanyId(refundruleRes.getResult().getShipmentCompanyId());
        ruleDto.setRefundWarehouseCode(refundruleRes.getResult().getRefundWarehouseCode());
        ruleDto.setRefundWarehouseName(warehousedto.getWarehouseName());
        return ruleDto;
    }

    /**
     * 根据code查询仓库
     */
    public WarehouseDTO findWarehouseByCodeandCompanyId(String warehouseCode) {
        String[] warehouseStrs = warehouseCode.split("-");
        Response<WarehouseDTO> warehouseRes = warehouseClient.findByOutCodeBizId(warehouseStrs[1],warehouseStrs[0]);
        if (!warehouseRes.isSuccess()) {
            log.error("find warehouse by code:{} fail,error:{}", warehouseCode, warehouseRes.getError());
            throw new JsonResponseException(warehouseRes.getError());
        }
        if(warehouseRes.getResult() != null){
            return warehouseRes.getResult();
        } else {
            log.error("find warehouse by code:{} is not exist", warehouseCode);
            throw new JsonResponseException(warehouseCode+"不存在此仓库");
        }
    }

    /**
     * 判断收货完成的售后换货单是否可以取消发货
     *
     * @param refund 售后单
     * @return 可以取消发货(true), 不可以取消发货(false)
     */
    public boolean isAfterSaleCanCancelShip(Refund refund) {
        Flow afterSaleFlow = flowPicker.pickAfterSales();
        Integer sourceStatus = refund.getStatus();
        return afterSaleFlow.operationAllowed(sourceStatus, MiddleOrderEvent.AFTER_SALE_CANCEL_SHIP.toOrderOperation());
    }

    /**
     * 判断售后换货单是否可以换转退
     *
     * @param refund 售后单
     * @return 可以换转退(true), 不可以换转退(false)
     */
    public boolean isExchangeTorefund(Refund refund) {
        Flow afterSaleFlow = flowPicker.pickAfterSales();
        Integer sourceStatus = refund.getStatus();
        return afterSaleFlow.operationAllowed(sourceStatus, MiddleOrderEvent.AFTER_SALE_EXCHANGE_TO_RETURN.toOrderOperation());
    }

    /**
     * 返回最多可退金额
     *
     * @param orderCode      订单id
     * @param refundId       退货单id
     * @param shipmentCode   发货单id
     * @param refundFeeDatas 传输进来的售后单sku以及数量
     * @return
     */
    public int getAlreadyRefundFee(String orderCode, Long refundId, String shipmentCode, List<RefundFeeData> refundFeeDatas) {
        Map<Long, Integer> requestSkuOrderId2Quantity = refundFeeDatas.stream()
                .collect(Collectors.toMap(RefundFeeData::getSkuOrderId, RefundFeeData::getApplyQuantity));
        //获取订单信息
        Response<List<Refund>> rltRes = refundReadService.findByOrderCodeAndOrderLevel(orderCode, OrderLevel.SHOP);
        if (!rltRes.isSuccess()) {
            log.error("find Refund failed,order id is ({}),caused by {}", orderCode, rltRes.getError());
            throw new JsonResponseException(rltRes.getError());
        }
        //获取不是订单号关联的其他售后单，过滤掉换货的，已经取消的，当前传入的需要计算的售后单
        List<Refund> refunds = rltRes.getResult().stream().filter(Objects::nonNull)
                .filter(refund -> !Objects.equals(refund.getId(), refundId))
                .filter(refund -> !Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_CHANGE.value()))
                .filter(refund -> !Objects.equals(refund.getRefundType(), MiddleRefundType.ON_SALES_REFUND.value()))
                .filter(refund -> !Objects.equals(refund.getRefundType(), MiddleRefundType.LOST_ORDER_RE_SHIPMENT.value()))
                .filter(refund -> !Objects.equals(refund.getStatus(), MiddleRefundStatus.CANCELED.getValue()))
                .filter(refund -> !Objects.equals(refund.getStatus(), MiddleRefundStatus.DELETED.getValue()))
                .collect(Collectors.toList());
        //获取已经退款的金额
        Long alreadyRefundFee = 0L;
        for (Refund refund : refunds) {
            RefundExtra refundExtra = this.findRefundExtra(refund);
            if (!Objects.equals(refundExtra.getShipmentId(), shipmentCode)) {
                continue;
            }
            alreadyRefundFee += refund.getFee();
        }
        Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(shipmentCode);
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
        //商品总净价
        Integer totalCleanFee = 0;
        //申请的商品总净价
        Integer totalEditCleanFee = 0;
        for (ShipmentItem shipmentItem : shipmentItems) {
            Long skuOrderId = shipmentItem.getSkuOrderId();
            totalCleanFee += shipmentItem.getCleanFee();
            if (requestSkuOrderId2Quantity.containsKey(skuOrderId)) {
                totalEditCleanFee += (shipmentItem.getCleanPrice() == null ? 0
                        : shipmentItem.getCleanPrice()) * requestSkuOrderId2Quantity.get(skuOrderId);
            }
        }
        //剩余最大可退金额
        Integer maxReturnFee = Math.toIntExact(totalCleanFee - alreadyRefundFee);
        return Math.min(totalEditCleanFee, maxReturnFee);
    }

    /**
     * 获取丢件补发类型的商品列表
     *
     * @param refund
     * @return
     */
    public List<RefundItem> findRefundLostItems(Refund refund) {
        Map<String, String> extraMap = refund.getExtra();
        if (CollectionUtils.isEmpty(extraMap)) {
            log.error("refund(id:{}) extra field is null", refund.getId());
            throw new JsonResponseException("refund.extra.is.empty");
        }
        if (!extraMap.containsKey(TradeConstants.REFUND_LOST_ITEM_INFO)) {
            log.error("refund(id:{}) extra map not contain key:{}", refund.getId(), TradeConstants.REFUND_LOST_ITEM_INFO);
            throw new JsonResponseException("refund.exit.not.contain.item.info");
        }
        return mapper.fromJson(extraMap.get(TradeConstants.REFUND_LOST_ITEM_INFO), mapper.createCollectionType(List.class, RefundItem.class));
    }

    /**
     * 通过outId获取渠道
     *
     * @return 获取渠道
     */
    public String getOutChannelTaobao(String outId) {
        if (StringUtils.hasText(outId)) {
            return Splitter.on('_').omitEmptyStrings().trimResults().limit(2).splitToList(outId).get(0);
        }
        return null;
    }

    /**
     * 通过outId获取渠道
     *
     * @return 获取渠道
     */
    public String getOutafterSaleIdTaobao(String outId) {
        if (StringUtils.hasText(outId)) {
            return Splitter.on('_').omitEmptyStrings().trimResults().limit(2).splitToList(outId).get(1);
        }
        return null;

    }

    /**
     * 判断丢件补发或者售后换货是否可以继续生成发货单
     *
     * @param refundItems
     * @return 可以继续生成发货单，则返回true，不可以继续生成发货单，返回false
     */
    public boolean checkRefundWaitHandleNumber(List<RefundItem> refundItems, Map<String, Integer> skuCodeAndQuantity) {
        int count = 0;
        if (refundItems.isEmpty()) {
            return count == 0;
        }
        for (RefundItem refundItem : refundItems) {
            //如果传入的传入的skuCode以及数量的map当中存在该skuCode才会判断已经申请的和初始状态的值是否已经小于等于0
            if (skuCodeAndQuantity.containsKey(refundItem.getSkuCode())) {
                if ((refundItem.getApplyQuantity() - (refundItem.getAlreadyHandleNumber() == null ? 0 : refundItem.getAlreadyHandleNumber())) <= 0) {
                    count++;
                }
            }
        }
        return count == 0;
    }

    /**
     * 判断丢件补发或者售后换货是否可以继续生成发货单
     *
     * @param refundItems
     * @return 可以继续生成发货单，则返回true，不可以继续生成发货单，返回false
     */
    public boolean checkRefundWaitHandleNumber2(List<RefundItem> refundItems, Map<String, Integer> skuCodeAndQuantity) {
        int count = 0;
        if (refundItems.isEmpty()) {
            return count == 0;
        }
        for (RefundItem refundItem : refundItems) {
            //如果传入的传入的skuCode以及数量的map当中存在该skuCode才会判断已经申请的和初始状态的值是否已经小于等于0
            if (skuCodeAndQuantity.containsKey(SkuCodeUtil.toCombineCode(refundItem))) {
                if ((refundItem.getApplyQuantity() - (refundItem.getAlreadyHandleNumber() == null ? 0 : refundItem.getAlreadyHandleNumber())) <= 0) {
                    count++;
                }
            }
        }
        return count == 0;
    }

    /**
     * 通过outId获取渠道
     *
     * @return 获取渠道
     */
    public String getOutChannelSuning(String outId) {
        if (StringUtils.hasText(outId)) {
            return Splitter.on('_').omitEmptyStrings().trimResults().limit(3).splitToList(outId).get(0);
        }
        return null;
    }

    public String getOutSkuOrderIdSuningSale(String outId) {
        if (StringUtils.hasText(outId)) {
            return Splitter.on('_').omitEmptyStrings().trimResults().limit(3).splitToList(outId).get(2);
        }
        return null;
    }


    /**
     * 查看发货单占用信息
     *
     * @param refund
     * @return
     */
    public List<RejectShipmentOccupy> getShipmentOccupies(Refund refund) {
        Map<String, String> extraMap = refund.getExtra();

        if (!extraMap.containsKey(TradeConstants.REJECT_SHIPMENT_OCCUPY_LIST)) {
            log.info("refund(id:{}) extra not contain key:{}", refund.getId(),
                    TradeConstants.REJECT_SHIPMENT_OCCUPY_LIST);
            return new ArrayList<>();
        }
        return mapper.fromJson(extraMap.get(TradeConstants.REJECT_SHIPMENT_OCCUPY_LIST),
                mapper.createCollectionType(List.class, RejectShipmentOccupy.class));
    }

    public Boolean checkShipInfoUnique(String shipmentSerialNo) {
        if (StringUtils.isEmpty(shipmentSerialNo)) {
            return true;
        }
        Response<List<Refund>> response = refundReadService.findByShipInfo(shipmentSerialNo);
        if (!response.isSuccess()) {
            log.error("fail to check ship info by shipmentSerialNo:{}, cause:{}",
                    shipmentSerialNo, response.getError());
            throw new JsonResponseException(response.getError());
        }
        List<Refund> list = response.getResult();
        if (!CollectionUtils.isEmpty(list)) {
            List<Integer> ignoreStatus = Lists.newArrayList(MiddleRefundStatus.WAIT_HANDLE.getValue(),
                    MiddleRefundStatus.CANCELED.getValue(), MiddleRefundStatus.DELETED.getValue());
            for (Refund refund : list) {
                if (!ignoreStatus.contains(refund.getStatus())) {
                    return false;
                }
            }
        }
        return true;
    }

    public Paging<Refund> pagingForFix(MiddleRefundCriteria criteria) {
        Response<Paging<Refund>> response = middleRefundReadService.paging(criteria);
        if (!response.isSuccess()) {
            log.error("paging refund by criteria:{} fail,error:{}", criteria, response.getError());
            throw new JsonResponseException("fix.refund.express.failed");
        }
        return response.getResult();
    }
}
