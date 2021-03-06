package com.pousheng.middle.web.order.sync.hk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.hksyc.pos.api.SycHkShipmentPosApi;
import com.pousheng.middle.hksyc.pos.dto.*;
import com.pousheng.middle.open.api.constant.ExtraKeyConstant;
import com.pousheng.middle.open.api.dto.YYEdiRefundConfirmItem;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.*;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.user.component.UcUserOperationLogic;
import com.pousheng.middle.web.user.dto.MemberProfile;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.order.dto.OpenClientPaymentInfo;
import io.terminus.parana.common.constants.JacksonType;
import io.terminus.parana.order.model.*;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * 同步恒康逆向订单逻辑 for 开pos单
 * Created by songrenfei on 2017/6/27
 */
@Slf4j
@Component
public class SyncRefundPosLogic {

    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private SycHkShipmentPosApi sycHkShipmentPosApi;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private WarehouseCacher warehouseCacher;
    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private UcUserOperationLogic ucUserOperationLogic;

    @Value("${pos.stock.code}")
    private String posStockCode;

    private static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();
    private static final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 同步恒康退货单
     *
     * @param refund 退货单
     * @return 同步结果 result 为恒康的退货单编号
     */
    public Response<Boolean> syncRefundPosToHk(Refund refund) {
        OrderRefund orderRefund = refundReadLogic.findOrderRefundByRefundId(refund.getId());
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderRefund.getOrderId());
        try {
            if (shopOrder.getShopName().startsWith("yj")) {
                if (!shopOrder.getExtra().containsKey(ExtraKeyConstant.IS_SYNCHK)
                        || Objects.equal("N", shopOrder.getExtra().get(ExtraKeyConstant.IS_SYNCHK))) {
                    return Response.ok();
                }
            }

            HkShipmentPosRequestData requestData = makeHkShipmentPosRequestData(refund);
            String url ="/common/erp/pos/addnetsalreturnstock";
            String result = sycHkShipmentPosApi.doSyncRefundPos(requestData,url);
            SycShipmentPosResponse response = JsonMapper.nonEmptyMapper().fromJson(result,SycShipmentPosResponse.class);
            if(!Objects.equal(response.getCode(),"00000")){
                log.error("sync refund(code:{}) pos to hk fail,error:{}",refund.getRefundCode(),response.getMessage());
                return Response.fail(response.getMessage());
            }
            return Response.ok();
        } catch (Exception e) {
            log.error("sync hk pos refund failed,refund is({}) cause by({})", refund.getId(), Throwables.getStackTraceAsString(e))          ;
            return Response.fail("sync.hk.pos.refund.fail");
        }

    }

    /**
     * 同步恒康拒收单信息
     * @param refund 售后单信息
     * @return
     */
    public Response<Boolean> syncSaleRefuseToHK(Refund refund){
        try{
            HkSaleRefuseRequestData requestData = makeHkSaleRefuseRequestData(refund);
            String url ="/common/pserp/pos/donetsalrefuse";
            String result = sycHkShipmentPosApi.doSyncSaleRefuse(requestData,url);
            SycShipmentPosResponse response = JsonMapper.nonEmptyMapper().fromJson(result,SycShipmentPosResponse.class);
            if(!Objects.equal(response.getCode(),"00000") && !Objects.equal(response.getCode(),"10010")){
                log.error("sync refund(code:{}) sale refuse to hk fail,error:{}",refund.getRefundCode(),response.getMessage());
                return Response.fail(response.getMessage());
            }
            return Response.ok();
        }catch (Exception e){
            log.error("sync  refund sale refuse failed,refund is({}) cause by({})", refund.getId(), Throwables.getStackTraceAsString(e))          ;
            return Response.fail("sync.sale.refuse.fail");
        }
    }

    /**
     * 组装拒收单请求参数
     * @param refund 售后单信息
     * @return
     */
    private HkSaleRefuseRequestData makeHkSaleRefuseRequestData(Refund refund){
        HkSaleRefuseRequestData requestData = new HkSaleRefuseRequestData();
        requestData.setTranReqDate(formatter.print(System.currentTimeMillis()));
        requestData.setSid("PS_ERP_POS_netsalrefuse");
        //组装content参数
        HkSaleRefuseContent bizContent  = this.makeHKSaleRefuseContent(refund);
        requestData.setBizContent(bizContent);
        return requestData;
    }
    private HkSaleRefuseContent makeHKSaleRefuseContent(Refund refund){
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        if(Arguments.isNull(refundExtra.getShipmentId())){
            log.error("refund(id:{}) shipment id invalid");
            throw new ServiceException("refund.shipment.id.invalid");
        }
        //获取发货单详情
        Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(refundExtra.getShipmentId());
        ShipmentDetail shipmentDetail = shipmentReadLogic.orderDetail(shipment.getId());

        HkSaleRefuseContent hkSaleRefuseContent = new HkSaleRefuseContent();
        //端点唯一订单号使用带前缀的单号
        hkSaleRefuseContent.setNetbillno(refund.getRefundCode());

        OpenShop openShop = orderReadLogic.findOpenShopByShopId(shipmentDetail.getShipment().getShopId());
        Map<String,String> openShopExtra = openShop.getExtra();
        //线上店铺code
        hkSaleRefuseContent.setNetshopcode(openShopExtra.get(TradeConstants.HK_PERFORMANCE_SHOP_OUT_CODE));
        //线上店铺所属公司id
        hkSaleRefuseContent.setNetcompanyid(openShopExtra.get(TradeConstants.HK_COMPANY_CODE));
        //发货单id
        hkSaleRefuseContent.setSourcebillno(shipmentDetail.getShipment().getShipmentCode());
        //客服备注
        hkSaleRefuseContent.setRemark(refund.getSellerNote());

        return hkSaleRefuseContent;
    }
    private HkShipmentPosRequestData makeHkShipmentPosRequestData(Refund refund){

        HkShipmentPosRequestData requestData = new HkShipmentPosRequestData();
        requestData.setTranReqDate(formatter.print(System.currentTimeMillis()));
        requestData.setSid("PS_ERP_POS_netsalreturnstock");
        HkShipmentPosContent bizContent = makeHkShipmentPosContent(refund);
        requestData.setBizContent(bizContent);
        return requestData;
    }

    private HkShipmentPosContent makeHkShipmentPosContent(Refund refund) {

        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        if(Arguments.isNull(refundExtra.getShipmentId())){
            log.error("refund(id:{}) shipment id invalid");
            throw new ServiceException("refund.shipment.id.invalid");
        }
        //获取发货单详情
        Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(refundExtra.getShipmentId());
        ShipmentDetail shipmentDetail = shipmentReadLogic.orderDetail(shipment.getId());
        HkShipmentPosContent posContent = new HkShipmentPosContent();

        posContent.setChanneltype("b2c");//订单来源类型, 是b2b还是b2c

        //退货仓
        if(Arguments.isNull(refundExtra.getWarehouseId())){
            log.error("refund(id:{}) refund warehouse not exist",refund.getId());
            throw new ServiceException("refund.warehouse.invalid");
        }
        WarehouseDTO warehouse = warehouseCacher.findById(refundExtra.getWarehouseId());
        if(StringUtils.isEmpty(warehouse.getOutCode())){
            log.error("warehouse(id:{}) out code invalid",warehouse.getId());
            throw new ServiceException("warehouse.out.code.invalid");
        }
        posContent.setNetcompanyid(warehouse.getCompanyId());//线上店铺所属公司id
        OpenShop openShop = orderReadLogic.findOpenShopByShopId(shipmentDetail.getShipment().getShopId());
        Map<String,String> openShopExtra = openShop.getExtra();
        posContent.setNetshopcode(openShopExtra.get(TradeConstants.HK_PERFORMANCE_SHOP_OUT_CODE));//线上店铺code

        posContent.setNetstockcode(posStockCode);//todo 线上店铺所属公司的虚拟仓代码
        posContent.setNetbillno(refund.getRefundCode());//端点唯一订单号使用带前缀的单号
        posContent.setSourcebillno("");//订单来源单号
        posContent.setBilldate(formatter.print(refund.getCreatedAt().getTime()));//订单日期
        posContent.setOperator("MPOS_EDI");//线上店铺帐套操作人code
        posContent.setRemark(refund.getBuyerNote());//备注
        if(Arguments.notNull(refundExtra.getIslock())){
            posContent.setIslock(String.valueOf(refundExtra.getIslock()));
        }

        HkShipmentPosInfo netsalorder = makeHkShipmentPosInfo(shipmentDetail,refund);
        //对于订单派发中心来说，可能有多个仓库，传订单派发中心的吧
        ShipmentExtra shipmentExtra = shipmentDetail.getShipmentExtra();
        //默认走仓库发货
        String shipmentWay = StringUtils.isEmpty(shipmentExtra.getShipmentWay())?"2":shipmentExtra.getShipmentWay();
        List<HkShipmentPosItem> ordersizes =null;
        if(isWarehouseShip(shipmentWay)){
            //仓库发货以wms具体退货仓为主
            if (Objects.equal(refund.getRefundType(), MiddleRefundType.AFTER_SALES_REFUND.value())){
                ordersizes = makeHKRefundPosItem(refund,shipmentDetail.getShipment().getShipmentCode());
            }else{
                if(!Objects.equal(refundExtra.getIslock(),1)){
                    ordersizes =makeHkShipmentPosItemWarehouse(refund,shipmentDetail.getShipment().getShipmentCode());
                }else {
                    ordersizes =makeHkShipmentPosItemWarehouseFormHk(refund,shipmentDetail.getShipment().getShipmentCode());
                }
            }
        }else{
            //ordersizes = makeHkShipmentPosItem(refund,shipmentDetail.getShipment().getShipmentCode(),extra.get("outCode"));
            ordersizes =makeHkShipmentPosItemWarehouse(refund,shipmentDetail.getShipment().getShipmentCode());
        }
        posContent.setNetsalorder(netsalorder);
        posContent.setOrdersizes(ordersizes);

        return posContent;
    }

    private List<HkShipmentPosItem> makeHkShipmentPosItem(Refund refund,String shipmentCode,String returnWarehouseCode) {

        List<RefundItem> refundItems = refundReadLogic.findRefundItems(refund);
        List<HkShipmentPosItem> posItems = Lists.newArrayListWithCapacity(refundItems.size());
        for (RefundItem refundItem : refundItems){
            HkShipmentPosItem hkShipmentPosItem = new HkShipmentPosItem();
            hkShipmentPosItem.setStockcode(returnWarehouseCode);
            hkShipmentPosItem.setSourcenetbillno(shipmentCode);
            hkShipmentPosItem.setMatbarcode(refundItem.getSkuCode());
            hkShipmentPosItem.setQty(refundItem.getApplyQuantity());
            hkShipmentPosItem.setBalaprice(new BigDecimal(refundItem.getCleanPrice()==null?0:refundItem.getCleanPrice()).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN).toString());
            posItems.add(hkShipmentPosItem);
        }
        return posItems;
    }

    private List<HkShipmentPosItem> makeHKRefundPosItem(Refund refund,String shipmentCode) {

        List<RefundItem> refundItems = refundReadLogic.findRefundItems(refund);
        List<HkShipmentPosItem> posItems = Lists.newArrayListWithCapacity(refundItems.size());
        for (RefundItem refundItem : refundItems){
            HkShipmentPosItem hkShipmentPosItem = new HkShipmentPosItem();
            hkShipmentPosItem.setStockcode("");
            hkShipmentPosItem.setSourcenetbillno(shipmentCode);
            hkShipmentPosItem.setMatbarcode(refundItem.getSkuCode());
            //仅退款传到恒康是0
            hkShipmentPosItem.setQty(0);
            //仅退款传到恒康是一个冲账的金额
            hkShipmentPosItem.setBalaprice(new BigDecimal(refund.getFee()).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN).toString());
            hkShipmentPosItem.setCouponprice(new BigDecimal(refundItem.getSharePlatformDiscount()==null?0:refundItem.getSharePlatformDiscount())
                    .divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN).toString());
            //售后不涉及到优惠
            posItems.add(hkShipmentPosItem);
        }
        return posItems;
    }

    /**
     * 仓库发货退货信息
     * @param refund
     * @param shipmentCode
     * @return
     */
    private List<HkShipmentPosItem> makeHkShipmentPosItemWarehouse(Refund refund,String shipmentCode) {
        List<YYEdiRefundConfirmItem> refundYYEdiConfirmItems = refundReadLogic.findRefundYYEdiConfirmItems(refund);
        List<RefundItem> refundItems = refundReadLogic.findRefundItems(refund);
        Map<String, String> skuCode2WarehouseCode = Maps.newHashMap();
        List<HkShipmentPosItem> posItems = Lists.newArrayListWithCapacity(refundYYEdiConfirmItems.size());
        for (YYEdiRefundConfirmItem yyEdiRefundConfirmItem : refundYYEdiConfirmItems) {
            // 即使有覆盖也不管了
            skuCode2WarehouseCode.put(yyEdiRefundConfirmItem.getItemCode(), yyEdiRefundConfirmItem.getWarhouseCode());
        }
        for (RefundItem refundItem:refundItems){
            Integer finalRefundQuantity = refundItem.getFinalRefundQuantity();
            //如果退货数量是0则过滤掉
            if (finalRefundQuantity == null || finalRefundQuantity == 0){
                continue;
            }
            HkShipmentPosItem hkShipmentPosItem = new HkShipmentPosItem();
            hkShipmentPosItem.setStockcode(skuCode2WarehouseCode.get(refundItem.getSkuCode()));
            hkShipmentPosItem.setSourcenetbillno(shipmentCode);
            hkShipmentPosItem.setMatbarcode(refundItem.getSkuCode());
            hkShipmentPosItem.setQty(finalRefundQuantity);
            Integer cleanPrice = refundItem.getCleanPrice();
            Integer sharePlatformDiscount = refundItem.getSharePlatformDiscount();
            hkShipmentPosItem.setBalaprice(new BigDecimal(cleanPrice == null ? 0 : cleanPrice)
                    .divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN).toString());
            hkShipmentPosItem.setCouponprice(new BigDecimal(sharePlatformDiscount == null ? 0 : sharePlatformDiscount)
                    .divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN).toString());
            //售后不涉及到优惠
            posItems.add(hkShipmentPosItem);
        }
        return posItems;
    }

    /**
     * 仓库发货退货信息
     * @param refund
     * @param shipmentCode
     * @return
     */
    private List<HkShipmentPosItem> makeHkShipmentPosItemWarehouseFormHk(Refund refund,String shipmentCode) {

        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        List<HkConfirmReturnItemInfo> hkConfirmItemInfos = refundExtra.getHkConfirmItemInfos();
        List<RefundItem> refundItems = refundReadLogic.findRefundItems(refund);
        Map<String,Integer> refundItemAndPriceMap = Maps.newHashMap();
        Map<String,Integer> skuCodeAndShareDiscountMap = Maps.newHashMap();
        for (RefundItem refundItem:refundItems){
            refundItemAndPriceMap.put(refundItem.getSkuCode(),refundItem.getCleanPrice());
            skuCodeAndShareDiscountMap.put(refundItem.getSkuCode(),refundItem.getSharePlatformDiscount());
        }
        List<HkShipmentPosItem> posItems = Lists.newArrayListWithCapacity(hkConfirmItemInfos.size());
        for (HkConfirmReturnItemInfo refundConfirmItem : hkConfirmItemInfos){
            //如果退货数量是0则过滤掉
            if (Objects.equal(refundConfirmItem.getQuantity(),"0")){
                continue;
            }
            HkShipmentPosItem hkShipmentPosItem = new HkShipmentPosItem();
            hkShipmentPosItem.setStockcode(refundConfirmItem.getWarhouseCode());
            hkShipmentPosItem.setSourcenetbillno(shipmentCode);
            hkShipmentPosItem.setMatbarcode(refundConfirmItem.getItemCode());
            hkShipmentPosItem.setQty(Integer.valueOf(refundConfirmItem.getQuantity()));
            hkShipmentPosItem.setBalaprice(new BigDecimal(refundItemAndPriceMap.get(refundConfirmItem.getItemCode())==null
                    ?0:refundItemAndPriceMap.get(refundConfirmItem.getItemCode())).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN).toString());
            //售后不涉及到优惠
            hkShipmentPosItem.setCouponprice(new BigDecimal(skuCodeAndShareDiscountMap.get(refundConfirmItem.getItemCode())==null
                    ?0:skuCodeAndShareDiscountMap.get(refundConfirmItem.getItemCode())).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN).toString());
            posItems.add(hkShipmentPosItem);
        }
        return posItems;
    }

    private HkShipmentPosInfo makeHkShipmentPosInfo(ShipmentDetail shipmentDetail,Refund refund) {

        ShopOrder shopOrder = shipmentDetail.getShopOrder();
        OpenClientPaymentInfo openClientPaymentInfo = orderReadLogic.getOpenClientPaymentInfo(shopOrder);
        ReceiverInfo receiverInfo = shipmentDetail.getReceiverInfo();
        ShipmentExtra shipmentExtra = shipmentDetail.getShipmentExtra();
        List<Invoice> invoices = shipmentDetail.getInvoices();

        HkShipmentPosInfo posInfo = new HkShipmentPosInfo();
        posInfo.setManualbillno(shopOrder.getOutId()); //第三方平台单号
        posInfo.setBuyeralipayno(""); //支付宝账号
        posInfo.setAlipaybillno(""); //支付交易号
        posInfo.setSourceremark(""); //订单来源说明
        posInfo.setOrdertype("");  //订单类型
        posInfo.setOrdercustomercode(""); //订单标记code
        posInfo.setAppamtsourceshop(""); //业绩来源店铺
        posInfo.setPaymentdate(formatter.print(shopOrder.getCreatedAt().getTime())); //付款时间

        //获取会员卡号
        String cardcode ="";
        if(Arguments.notNull(shopOrder.getBuyerId())&&shopOrder.getBuyerId()>0L&&shopOrder.getShopName().startsWith("官网")){
            Response<MemberProfile>  memberProfileRes = ucUserOperationLogic.findByUserId(shopOrder.getBuyerId());
            if(!memberProfileRes.isSuccess()){
                log.error("find member profile by user id:{} fail,error:{}",shopOrder.getBuyerId(),memberProfileRes.getError());
            }else {
                cardcode = memberProfileRes.getResult().getOuterId();
            }
        }


        posInfo.setCardcode(cardcode); //会员卡号
        posInfo.setBuyercode(shopOrder.getBuyerName());//买家昵称
        posInfo.setBuyermobiletel(shopOrder.getOutBuyerId()); //买家手机
        posInfo.setBuyertel(""); //买家座机
        posInfo.setBuyerremark(shopOrder.getBuyerNote()); //买家留言
        posInfo.setConsigneename(receiverInfo.getReceiveUserName());//收货人姓名
        posInfo.setPayamountbakup(new BigDecimal(refund.getFee()==null?0:refund.getFee()).divide(new BigDecimal(100),2, RoundingMode.HALF_DOWN).toString()); //线上实付金额
        posInfo.setZramount("0");
        posInfo.setExpresscost("0");//邮费成本
        posInfo.setCodcharges("0");//货到付款服务费
        posInfo.setPreremark(""); //优惠信息
        if(CollectionUtils.isEmpty(invoices)){
            posInfo.setIsinvoice("0"); //是否开票

        }else {
            posInfo.setIsinvoice("1"); //是否开票
            posInfo.setInvoice_name(invoices.get(0).getTitle()); //发票抬头
            posInfo.setTaxno("");//税号
        }

        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        //卖家备注
        String sellerNote =refund.getSellerNote();
        posInfo.setProvince(receiverInfo.getProvince()); //省
        posInfo.setCity(receiverInfo.getCity()); //市
        posInfo.setArea(receiverInfo.getRegion()); //区
        posInfo.setZipcode(receiverInfo.getPostcode());//邮政编码
        posInfo.setAddress(receiverInfo.getDetail()); //详细地址
        posInfo.setSellremark(sellerNote);//卖家备注
        posInfo.setSellcode(shopOrder.getBuyerName()); //卖家昵称
        posInfo.setExpresstype("express");//物流方式
        posInfo.setVendcustcode(refundExtra.getShipmentCorpCode());  //物流公司代码 寄回物流公司代码
        posInfo.setExpressbillno(refundExtra.getShipmentSerialNo()); //物流单号 寄回物流单号
        posInfo.setWms_ordercode(""); //第三方物流单号
        if(Arguments.isNull(shipmentExtra.getShipmentDate())){
            posInfo.setConsignmentdate(formatter.print(System.currentTimeMillis())); //发货时间
        }else {
            posInfo.setConsignmentdate(formatter.print(shipmentExtra.getShipmentDate().getTime())); //发货时间

        }

        return posInfo;
    }

    private String getBillNo(String data) throws Exception{
        Map<String,String> map =objectMapper.readValue(data, JacksonType.MAP_OF_STRING);
        if(!CollectionUtils.isEmpty(map)&&map.containsKey("billNo")){
            return map.get("billNo");
        } else{
            throw new ServiceException("hk.result.bill.no.invalid");
        }
    }

    private Boolean isWarehouseShip(String shipmentWay){
        if(Objects.equal(shipmentWay,"2")){
            return Boolean.TRUE;
        }else {
            return Boolean.FALSE;
        }
    }

}
