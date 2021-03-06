package com.pousheng.middle.web.trade;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.pousheng.middle.hksyc.component.SycHkOrderCancelApi;
import com.pousheng.middle.hksyc.component.SycHkRefundOrderApi;
import com.pousheng.middle.hksyc.component.SycHkShipmentOrderApi;
import com.pousheng.middle.hksyc.dto.trade.*;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.order.dto.OpenFullOrder;
import io.terminus.open.client.order.dto.OpenFullOrderAddress;
import io.terminus.open.client.order.dto.OpenFullOrderInfo;
import io.terminus.open.client.order.dto.OpenFullOrderItem;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.List;
import java.util.Map;

/**
 * Created by songrenfei on 2017/7/5
 */
@Slf4j
public class SyncHkTest {

    @Test
    public void testEsb(){
        String url ="https://esbt.pousheng.com/common/terminus/base/gethelloworld?name=1923311113";
        String result = HttpRequest.get(url).trustAllHosts().trustAllCerts().header("verifycode","e153ca58197e4931977f6a17a27f0beb").connectTimeout(1000000).readTimeout(1000000).body();
        System.out.println(result);

    }

    protected Map<String, Object> params = Maps.newTreeMap();

    @Test
    public void testHkSyncShipment(){

        params.put("appKey","pousheng");
        params.put("pampasCall","hk.shipments.api");
        params.put("shipmentId","92");
        params.put("hkShipmentId","92");
        params.put("shipmentCorpCode","hkshunfeng");
        params.put("shipmentSerialNo","7423333332");
        params.put("shipmentDate","20160625224210");
        params.put("posSerialNo","123455687");
        params.put("posType","2");
        params.put("posAmt","234.75");
        params.put("posCreatedAt","20170625224210");
        String sign = sign("middle");
        System.out.println("==============sign: "+sign);
        params.put("sign",sign);

        log.info(JsonMapper.nonDefaultMapper().toJson(params));;
        post(middleUrl());

    }

    @Test
    public void testHkSyncOrderToEcp(){

        params.put("appKey","pousheng");
        params.put("pampasCall","push.out.open.order.api");
        List<OpenFullOrderInfo> orderInfos = Lists.newArrayList();
        OpenFullOrderInfo openFullOrderInfo = new OpenFullOrderInfo();
        OpenFullOrder openFullOrder = new OpenFullOrder();
        openFullOrder.setBuyerMobile("15152306789");
        openFullOrder.setOutOrderId("1334");
        openFullOrder.setCompanyCode("244");
        openFullOrder.setOutId("244-1334");
        openFullOrder.setShopCode("SP110073");
        openFullOrder.setPerformanceShopCode("SP110073");
        openFullOrder.setFee(28800L);
        openFullOrder.setOriginFee(28800L);
        openFullOrder.setDiscount(0L);
        openFullOrder.setShipFee(0L);
        openFullOrder.setOriginShipFee(0L);
        openFullOrder.setIntegral(0L);
        openFullOrder.setShipmentType(1);
        openFullOrder.setPayType(1);
        openFullOrder.setOrderChannel(1);
        openFullOrder.setType(1);
        openFullOrder.setBuyerNote("");
        openFullOrder.setStatus(1);
        openFullOrder.setChannel("hk");
        openFullOrder.setPaymentDate("20180101123302");
        openFullOrder.setCreatedAt("20180101123302");
        List<OpenFullOrderItem> items =  Lists.newArrayList();
        OpenFullOrderItem item = new OpenFullOrderItem();
        item.setOutSkuorderId("1334");
        item.setSkuCode("4057289618927");
        item.setItemType("01");
        item.setItemName("测试商品");
        item.setQuantity(1);
        item.setOriginFee(28800L);
        item.setDiscount(0L);
        item.setCleanPrice(28800L);
        item.setCleanFee(28800L);
        items.add(item);
        OpenFullOrderAddress address = new OpenFullOrderAddress();
        address.setProvince("江苏省");
        address.setCity("南京市");
        address.setRegion("江宁区");
        address.setStreet("");
        address.setDetail("胜利路89号");
        address.setEmail("zhaoxiaotao@terminus.io");
        address.setMobile("18021529596");
        address.setPhone("02512345678");
        address.setReceiveUserName("易秋涵");
        address.setPostcode("214092");
        openFullOrderInfo.setOrder(openFullOrder);
        openFullOrderInfo.setItem(items);
        openFullOrderInfo.setAddress(address);
        orderInfos.add(openFullOrderInfo);
        String paramJson = JsonMapper.nonEmptyMapper().toJson(orderInfos);
        params.put("orderInfo",paramJson);
        String sign = sign("6a0e@93204aefe45d47f6e488");
        System.out.println("==============sign: "+sign);
        params.put("sign",sign);

        log.info(JsonMapper.nonDefaultMapper().toJson(params));;
        post(middleUrl());

    }

    @Test
    public void testHkSyncPos(){

        params.put("appKey","pousheng");
        params.put("pampasCall","hk.pos.api");
        params.put("orderId","1");
        params.put("orderType","2");
        params.put("posSerialNo","88888888");
        params.put("posType","1");
        params.put("posAmt","7777");
        params.put("posCreatedAt","20170625224210");
        String sign = sign("middle");
        System.out.println("==============sign: "+sign);
        params.put("sign",sign);

        log.info(JsonMapper.nonDefaultMapper().toJson(params));;
        post(middleUrl());

    }

    @Test
    public void testHkSyncRefund(){

        params.put("appKey","pousheng");
        params.put("pampasCall","hk.refund.confirm.received.api");
        params.put("refundOrderId","3");
        params.put("hkRefundOrderId","3");
        params.put("itemInfo","{\"refundExtraInfo\":\"{\\\"shipmentId\\\":23,\\\"receiverInfo\\\":{\\\"id\\\":392,\\\"userId\\\":3,\\\"receiveUserName\\\":\\\"buyer\\\",\\\"mobile\\\":\\\"15763948284\\\",\\\"isDefault\\\":true,\\\"status\\\":1,\\\"province\\\":\\\"天津\\\",\\\"provinceId\\\":120000,\\\"city\\\":\\\"天津市\\\",\\\"cityId\\\":120100,\\\"region\\\":\\\"和平区\\\",\\\"regionId\\\":120101,\\\"detail\\\":\\\"ffffsssssssshjh\\\",\\\"createdAt\\\":1482645231000,\\\"updatedAt\\\":1484644903000},\\\"handleDoneAt\\\":1500449189480}\",\"refundItemInfo\":\"[{\\\"skuCode\\\":\\\"W07090020S\\\",\\\"outSkuCode\\\":\\\"W07090020\\\",\\\"skuName\\\":\\\"测试新产品1\\\",\\\"cleanFee\\\":10000,\\\"attrs\\\":[{\\\"attrKey\\\":\\\"颜色\\\",\\\"attrVal\\\":\\\"深蓝色\\\",\\\"showImage\\\":false},{\\\"attrKey\\\":\\\"尺码\\\",\\\"attrVal\\\":\\\"S\\\",\\\"showImage\\\":false}],\\\"skuOrderId\\\":1,\\\"applyQuantity\\\":5,\\\"skuPrice\\\":2}]\"}");
        params.put("receivedDate","20160625224210");
        params.put("posSerialNo","222555655");
        params.put("posType","1");
        params.put("posAmt","32566.12");
        params.put("posCreatedAt","20160625224210");
        String sign = sign("middle");
        System.out.println("==============sign: "+sign);
        params.put("sign",sign);

        log.info(JsonMapper.nonDefaultMapper().toJson(params));;
        post(middleUrl());

    }



    @Test
    public void testHkShipment(){
        SycHkShipmentOrderApi api = new SycHkShipmentOrderApi();

        List<SycHkShipmentOrderDto> orders = Lists.newArrayList();

        SycHkShipmentOrder sycHkShipmentOrder = new SycHkShipmentOrder();
        SycHkUserAddress sycHkUserAddress = new SycHkUserAddress();
        SycHkShipmentItem sycHkShipmentItem = new SycHkShipmentItem();
        List<SycHkShipmentItem> items = Lists.newArrayList();
        items.add(sycHkShipmentItem);
        sycHkShipmentOrder.setItems(items);

        SycHkShipmentOrderDto dto = new SycHkShipmentOrderDto();
        dto.setTradeOrder(sycHkShipmentOrder);
        dto.setUserAddress(sycHkUserAddress);
        orders.add(dto);

        api.doSyncShipmentOrder(orders,sycHkShipmentOrder.getOrderNo());
    }


    @Test
    public void testOrderCancel(){
        SycHkOrderCancelApi api = new SycHkOrderCancelApi();
        api.doCancelOrder("SHP12121",0);
    }

    @Test
    public void testSycHkRefundOrder(){
        SycHkRefundOrderApi api = new SycHkRefundOrderApi();
        SycHkRefund sycHkRefund = new SycHkRefund();
        List<SycHkRefundItem> sycHkRefundItems = Lists.newArrayList();
        SycHkRefundItem sycHkRefundItem = new SycHkRefundItem();
        sycHkRefundItems.add(sycHkRefundItem);
        api.doSyncRefundOrder(sycHkRefund,sycHkRefundItems);
    }


    @Test
    public void testEsb2(){
        String url ="https://esbt.pousheng.com/common-terminus/ec/default/gethelloworld?name=1923311113";
        String result = HttpRequest.get(url).trustAllHosts().trustAllCerts().header("verifycode","e153ca58197e4931977f6a17a27f0beb").connectTimeout(1000000).readTimeout(1000000).body();
        System.out.println(result);

    }


    /**
     * 对参数列表进行签名
     */
    public String sign(String secret) {
        try {
            String toVerify = Joiner.on('&').withKeyValueSeparator("=").join(params);

            String sign = Hashing.md5().newHasher()
                    .putString(toVerify, Charsets.UTF_8)
                    .putString(secret, Charsets.UTF_8).hash().toString();

            return sign;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String middleUrl() {
        String suffix = Joiner.on('&').withKeyValueSeparator("=").join(params);
        String url =  "http://middle-api-test.pousheng.com/api/gateway" + "?" + suffix;
        System.out.println(url);
        return url;
    }

    public void post(String url){
        String result = HttpRequest.post(url).connectTimeout(1000000).readTimeout(1000000).body();
        System.out.println(result);
    }
}
