package com.pousheng.middle.web.order;

import com.google.common.collect.Maps;
import com.pousheng.middle.item.enums.ShopType;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.RefundItem;
import com.pousheng.middle.order.dto.ShipmentPreview;
import com.pousheng.middle.order.dto.ShipmentRequest;
import com.pousheng.middle.order.dto.WaitShipItemInfo;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddlePayType;
import com.pousheng.middle.shop.cacher.MiddleShopCacher;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.WarehouseClient;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.warehouse.enums.WarehouseType;
import com.pousheng.middle.web.item.cacher.VipWarehouseMappingProxy;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.parana.common.exception.InvalidException;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.ReceiverInfoReadService;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 销售发货  和 换货发货 合并api
 * Created by songrenfei on 2017/7/6
 */
@RestController
@Slf4j
public class CreateShipments {

    @Autowired
    private AdminOrderReader adminOrderReader;
    @Autowired
    private Refunds refunds;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private WarehouseClient warehouseClient;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @Autowired
    private MiddleShopCacher middleShopCacher;
    @Autowired
    private WarehouseCacher warehouseCacher;
    @Autowired
    private VipWarehouseMappingProxy vipWarehouseMappingProxy;
    @RpcConsumer
    private ReceiverInfoReadService receiverInfoReadService;


    /**
     * 待处理商品列表
     *
     * @param id   单据id
     * @param type 1 销售发货  2 换货发货
     * @return 商品信息
     */
    @RequestMapping(value = "/api/wait/handle/sku", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<WaitShipItemInfo> waitHandleSku(@RequestParam Long id, @RequestParam(defaultValue = "1") Integer type) {
        if (log.isDebugEnabled()) {
            log.debug("API-WAIT-HANDLE-SKU-START param: id [{}] type [{}]", id, type);
        }
        if (Objects.equals(1, type)) {
            return adminOrderReader.orderWaitHandleSku(id);
        }
        List<WaitShipItemInfo> waitShipItemInfos = refunds.refundWaitHandleSku(id);
        if (log.isDebugEnabled()) {
            log.debug("API-WAIT-HANDLE-SKU-END param: id [{}] type [{}] ,resp: [{}]", id, type, JsonMapper.nonEmptyMapper().toJson(waitShipItemInfos));
        }
        return waitShipItemInfos;

    }

    /**
     * @param id   单据id
     * @param type 1 销售发货  2 换货发货
     * @return 商品信息
     */
    @RequestMapping(value = "/api/wait/handle/shop/id", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Long> findShopId(@RequestParam Long id, @RequestParam(defaultValue = "1") Integer type) {
        if (log.isDebugEnabled()) {
            log.debug("API-WAIT-HANDLE-SHOP-START param: id [{}] type [{}]", id, type);
        }
        if (Objects.equals(1, type)) {
            OrderBase orderBase = orderReadLogic.findOrder(id, OrderLevel.SHOP);
            if (Arguments.isNull(orderBase)) {
                log.error("not find order by id:{}", id);
                return Response.fail("order.not.exist");
            }
            return Response.ok(orderBase.getShopId());
        }

        Refund refund = refundReadLogic.findRefundById(id);
        if (Arguments.isNull(refund)) {
            log.error("not find refund by id:{}", id);
            return Response.fail("refund.not.exist");
        }
        return Response.ok(refund.getShopId());

    }


    /**
     * @param code 单据code
     * @param type 1 销售发货  2 换货发货
     * @return 商品信息
     */
    @RequestMapping(value = "/api/query/wait/handle/shop/id", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Long> findShopIdByOrderCode(@RequestParam String code, @RequestParam(defaultValue = "1") Integer type) {
        if (log.isDebugEnabled()) {
            log.debug("API-WAIT-HANDLE-SHOP-START param: id [{}] code [{}]", code, type);
        }
        if (Objects.equals(1, type)) {
            ShopOrder orderBase = orderReadLogic.findShopOrderByCode(code);
            if (Arguments.isNull(orderBase)) {
                log.error("not find order by code:{}", code);
                return Response.fail("order.not.exist");
            }
            return Response.ok(orderBase.getShopId());
        }

        Refund refund = refundReadLogic.findRefundByRefundCode(code);
        if (Arguments.isNull(refund)) {
            log.error("not find refund by code:{}", code);
            return Response.fail("refund.not.exist");
        }
        return Response.ok(refund.getShopId());

    }


    /**
     * 发货预览
     *
     * @param id       单据id
     * @param dataList 请求的skuCode-quantity 和发货仓id
     * @param type     1 销售发货  2 换货发货，3.丢件补发
     * @return 批量订单信息
     */
    @RequestMapping(value = "/api/ship/preview", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<List<ShipmentPreview>> shipPreview(@RequestParam Long id,
                                                       @RequestParam(value = "dataList") String dataList,
                                                       @RequestParam(defaultValue = "1") Integer type) {
        if (log.isDebugEnabled()) {
            log.debug("API-SHIP-PREVIEW-START param: id [{}] dataList [{}] type [{}]", id, dataList, type);
        }
        List<ShipmentRequest> requestDataList = JsonMapper.nonEmptyMapper().fromJson(dataList, JsonMapper.nonEmptyMapper().createCollectionType(List.class, ShipmentRequest.class));
        if (Arguments.isNull(requestDataList)) {
            log.error("data json :{} invalid", dataList);
            throw new JsonResponseException("analysis.shipment.json.error");
        }
        if (Objects.equals(type, 1)) {
            ShopOrder shopOrder = orderReadLogic.findShopOrderById(id);
            Response<List<ReceiverInfo>> receiveInfosRes = receiverInfoReadService.findByOrderId(shopOrder.getId(), OrderLevel.SHOP);
            if (!receiveInfosRes.isSuccess()) {
                throw new JsonResponseException(receiveInfosRes.getError());
            }
            ReceiverInfo receiverInfo = receiveInfosRes.getResult().get(0);
            if (Arguments.isNull(receiverInfo.getProvince()) || Arguments.isNull(receiverInfo.getCity())) {
                throw new JsonResponseException("order.receiver.address.is.not.complete");
            }
            List<SkuOrder> skuOrders = orderReadLogic.findSkuOrdersByShopOrderId(id);
            //京东货到付款订单不允许拆分
            if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.JD.getValue())
                    && Objects.equals(shopOrder.getPayType(), MiddlePayType.CASH_ON_DELIVERY.getValue())) {
                if (requestDataList.size() > 1 || requestDataList.get(0).getData().size() != skuOrders.size()) {
                    log.info("data json :{} invalid", dataList);
                    throw new JsonResponseException("jingdong.delivery.cannot.dispatch");
                }
            }
            //唯品会的单子不允许拆单 且必须用唯品会的仓库
            if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.VIPOXO.getValue())) {
                if (requestDataList.size() > 1 || requestDataList.get(0).getData().size() != skuOrders.size()) {
                    log.info("data json :{} invalid", dataList);
                    throw new JsonResponseException("vip.delivery.cannot.dispatch");
                }
                WarehouseDTO warehouseDTO = warehouseCacher.findById(requestDataList.get(0).getWarehouseId());
                if (vipWarehouseMappingProxy.findByWarehouseId(warehouseDTO.getId()) == null) {
                    log.error("warehouse is not vip warehouse:{} fail", warehouseDTO.getId());
                    throw new InvalidException(500, "not.vip.warehouse(outCode={0})", warehouseDTO.getOutCode());
                }
            }
        } else {
            //售后单的发货单不允许数量级拆单
            Refund refund = refundReadLogic.findRefundById(id);
            List<RefundItem> list = Lists.newArrayList();
            if (type == 2) {
                list = refundReadLogic.findRefundChangeItems(refund);
            }
            if (type == 3) {
                list = refundReadLogic.findRefundLostItems(refund);
            }

            // 考虑 skucode 相同情况
            Map<String, Integer> refundItemMap = Maps.newHashMap();
            list.forEach(it -> {
                Integer q = refundItemMap.getOrDefault(it.getSkuCode(), 0);
                refundItemMap.put(it.getSkuCode(), q + it.getApplyQuantity());
            });

            for (ShipmentRequest request : requestDataList) {
                Set<Object> skuCodes = request.getData().keySet();
                for (Object o : skuCodes) {
                    if (refundItemMap.get(o.toString()) == null) {
                        throw new InvalidException(500, "refund.item.not.contain.sku.code");
                    }
                    if (request.getData().get(o) != 0 && request.getData().get(o) < refundItemMap.get(o.toString())) {
                        log.error("refund shipment  not allow part shopping  {} ,{}", request.getData().get(o), refundItemMap.get(o.toString()));
                        throw new InvalidException(500, "refund.can,not.part.shipping");
                    }
                }
            }
        }
        //用于判断运费是否计算
        int shipmentFeeCount = 0;
        List<ShipmentPreview> shipmentPreviews = Lists.newArrayList();
        for (ShipmentRequest shipmentRequest : requestDataList) {
            String data = JsonMapper.nonEmptyMapper().toJson(shipmentRequest.getData());
            Long warehouseId = shipmentRequest.getWarehouseId();
            Response<ShipmentPreview> response;
            if (Objects.equals(1, type)) {
                response = shipmentReadLogic.orderShipPreview(id, data);
            } else if (Objects.equals(2, type) || Objects.equals(3, type)) {
                response = shipmentReadLogic.changeShipPreview(id, data);
            } else {
                throw new JsonResponseException("invalid.type");
            }

            if (!response.isSuccess()) {
                throw new JsonResponseException(response.getError());
            }


            //封装发货仓及下单店铺信息
            ShipmentPreview shipmentPreview = response.getResult();

            //发货仓库信息
            Response<WarehouseDTO> warehouseRes = warehouseClient.findById(warehouseId);
            if (!warehouseRes.isSuccess()) {
                log.error("find warehouse by id:{} fail,error:{}", warehouseId, warehouseRes.getError());
                throw new JsonResponseException(warehouseRes.getError());
            }

            WarehouseDTO warehouse = warehouseRes.getResult();
            //店仓要判断对应店铺的类型是否为接单店铺
            if (warehouse.getWarehouseSubType().equals(WarehouseType.SHOP_WAREHOUSE.value())) {
                Shop shop = middleShopCacher.findByOuterIdAndBusinessId(warehouse.getOutCode(), Long.parseLong(warehouse.getCompanyId()));
                if (ShopType.ORDERS_SHOP.value() == shop.getType()) {
                    log.info("shop({}).type.abnormal", shop.getId());
                    throw new JsonResponseException("shop.type.abnormal");
                }
            }
            shipmentPreview.setWarehouseId(warehouse.getId());
            shipmentPreview.setWarehouseName(warehouse.getWarehouseName());
            //判断所选仓库是否数据下单店铺的账套
            OpenShop openShop = orderReadLogic.findOpenShopByShopId(shipmentPreview.getShopId());
           /* String erpType = orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.ERP_SYNC_TYPE,openShop);
            if (StringUtils.isEmpty(erpType)||Objects.equals(erpType,"hk")){
                if (!orderReadLogic.validateCompanyCode(warehouseId,shipmentPreview.getShopId())){
                    throw new JsonResponseException("warehouse.must.be.in.one.company");
                }
            }*/
            String shopCode = orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.HK_PERFORMANCE_SHOP_CODE, openShop);
            String shopName = orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.HK_PERFORMANCE_SHOP_NAME, openShop);
            String shopOutCode = orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.HK_PERFORMANCE_SHOP_OUT_CODE, openShop);
            shipmentWiteLogic.defaultPerformanceShop(openShop, shopCode, shopName, shopOutCode);
            shipmentPreview.setErpOrderShopCode(shopCode);
            shipmentPreview.setErpOrderShopName(shopName);
            //如果订单extra表中存在绩效店铺编码，直接去shopOrderExtra中的绩效店铺编码

            shipmentPreview.setErpPerformanceShopCode(shopCode);
            shipmentPreview.setErpPerformanceShopName(shopName);

            //计算金额
            //发货单商品金额
            Long shipmentItemFee = 0L;
            //发货单总的优惠
            Long shipmentDiscountFee = 0L;
            //发货单总的净价
            Long shipmentTotalFee = 0L;
            //运费
            Long shipmentShipFee = 0L;
            //运费优惠
            Long shipmentShipDiscountFee = 0L;
            if (Objects.equals(1, type)) {
                ShopOrder shopOrder = orderReadLogic.findShopOrderById(id);
                //判断运费是否已经加过
                if (!shipmentReadLogic.isShipmentFeeCalculated(id) && shipmentFeeCount == 0) {
                    shipmentShipFee = Long.valueOf(shopOrder.getOriginShipFee() == null ? 0 : shopOrder.getOriginShipFee());
                    shipmentShipDiscountFee = shipmentShipFee - Long.valueOf(shopOrder.getShipFee() == null ? 0 : shopOrder.getShipFee());
                    shipmentFeeCount++;
                }
                shipmentPreview.setShipmentShipFee(shipmentShipFee);
            }
            List<ShipmentItem> shipmentItems = shipmentPreview.getShipmentItems();
            for (ShipmentItem shipmentItem : shipmentItems) {
                shipmentItemFee = shipmentItem.getSkuPrice() * shipmentItem.getQuantity() + shipmentItemFee;
                shipmentDiscountFee = (shipmentItem.getSkuDiscount() == null ? 0 : shipmentItem.getSkuDiscount()) + shipmentDiscountFee;
                shipmentTotalFee = shipmentItem.getCleanFee() + shipmentTotalFee;
            }
            //发货单总金额(商品总净价+运费)
            Long shipmentTotalPrice = shipmentTotalFee + shipmentShipFee - shipmentShipDiscountFee;
            shipmentPreview.setShipmentItemFee(shipmentItemFee);
            shipmentPreview.setShipmentDiscountFee(shipmentDiscountFee);
            shipmentPreview.setShipmentTotalFee(shipmentTotalFee);
            shipmentPreview.setShipmentShipDiscountFee(shipmentShipDiscountFee);
            shipmentPreview.setShipmentTotalPrice(shipmentTotalPrice);
            shipmentPreviews.add(shipmentPreview);
        }
        if (log.isDebugEnabled()) {
            log.debug("API-SHIP-PREVIEW-END param: id [{}] dataList [{}] type [{}] ,resp: [{}]", id, dataList, type, JsonMapper.nonEmptyMapper().toJson(shipmentPreviews));
        }
        return Response.ok(shipmentPreviews);
    }

}
