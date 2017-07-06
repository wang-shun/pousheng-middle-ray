package com.pousheng.middle.web.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.MiddleOrderCriteria;
import com.pousheng.middle.order.dto.ShopOrderPagingInfo;
import com.pousheng.middle.order.dto.ShopOrderWithReceiveInfo;
import com.pousheng.middle.order.dto.WaitShipItemInfo;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import com.pousheng.middle.web.order.component.MiddleOrderFlowPicker;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.OrderDetail;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.ReceiverInfoReadService;
import io.terminus.parana.order.service.ShopOrderReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * Mail: F@terminus.io
 * Data: 16/6/28
 * Author: yangzefeng
 */
@RestController
@Slf4j
public class AdminOrderReader {


    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @RpcConsumer
    private MiddleOrderReadService middleOrderReadService;
    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;
    @RpcConsumer
    private ReceiverInfoReadService receiverInfoReadService;
    @Autowired
    private MiddleOrderFlowPicker flowPicker;


    /**
     * 交易订单分页
     * @param middleOrderCriteria 查询参数
     * @return 订单分页结果（不包含操作按钮）
     */
    @RequestMapping(value = "/api/order/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Paging<ShopOrderPagingInfo>> findBy(MiddleOrderCriteria middleOrderCriteria) {

        Response<Paging<ShopOrder>> pagingRes =  middleOrderReadService.pagingShopOrder(middleOrderCriteria);
        if(!pagingRes.isSuccess()){
            return Response.fail(pagingRes.getError());
        }
        Flow flow = flowPicker.pickOrder();
        List<ShopOrder> shopOrders = pagingRes.getResult().getData();
        Paging<ShopOrderPagingInfo> pagingInfoPaging = Paging.empty();
        List<ShopOrderPagingInfo> pagingInfos = Lists.newArrayListWithCapacity(shopOrders.size());
        shopOrders.forEach(shopOrder -> {
            ShopOrderPagingInfo shopOrderPagingInfo = new ShopOrderPagingInfo();
            shopOrderPagingInfo.setShopOrder(shopOrder);
            shopOrderPagingInfo.setShopOrderOperations(flow.availableOperations(shopOrder.getStatus()));
            pagingInfos.add(shopOrderPagingInfo);
        });

        pagingInfoPaging.setData(pagingInfos);
        pagingInfoPaging.setTotal(pagingRes.getResult().getTotal());

        return Response.ok(pagingInfoPaging);

    }


    /**
     * 交易订单详情
     * @param id 交易订单id
     * @return 订单详情DTO
     */
    @RequestMapping(value = "/api/order/{id}/detail", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<OrderDetail> detail(@PathVariable("id") Long id) {
        return orderReadLogic.orderDetail(id);
    }


    /**
     * 交易订单待处理商品列表 for 手动生成发货单流程的选择仓库页面
     * @param id 交易订单id
     * @return 待发货商品列表 注意：待发货数量(waitHandleNumber) = 下单数量 - 已发货数量 ,waitHandleNumber为skuOrder.extraMap中的一个key，value为待发货数量
     */
    @RequestMapping(value = "/api/order/{id}/wait/handle/sku", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<WaitShipItemInfo> orderWaitHandleSku(@PathVariable("id") Long id) {
        List<SkuOrder> skuOrders =  orderReadLogic.findSkuOrderByShopOrderIdAndStatus(id, MiddleOrderStatus.WAIT_HANDLE.getValue());
        List<WaitShipItemInfo> waitShipItemInfos = Lists.newArrayListWithCapacity(skuOrders.size());
        for (SkuOrder skuOrder : skuOrders){
            WaitShipItemInfo waitShipItemInfo = new WaitShipItemInfo();
            waitShipItemInfo.setSkuOrderId(skuOrder.getId());
            waitShipItemInfo.setSkuCode(skuOrder.getSkuCode());
            waitShipItemInfo.setOutSkuCode(skuOrder.getOutSkuId());
            waitShipItemInfo.setSkuName(skuOrder.getItemName());
            waitShipItemInfo.setWaitHandleNumber(Integer.valueOf(orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.WAIT_HANDLE_NUMBER,skuOrder)));
            waitShipItemInfos.add(waitShipItemInfo);
        }
        return waitShipItemInfos;
    }


    /**
     * 判断交易订单是否存在
     * @param id 交易订单id
     * @return boolean类型 ，true为存在，false为不存在
     */
    @RequestMapping(value = "/api/order/{id}/exist", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean checkExist(@PathVariable("id") Long id) {

        Response<ShopOrder> shopOrderRes = shopOrderReadService.findById(id);
        if(!shopOrderRes.isSuccess()){
            log.error("find shop order by id:{} fail,error:{}",id,shopOrderRes.getError());
            if(Objects.equals(shopOrderRes.getError(),"order.not.found")){
                return Boolean.FALSE;
            }
            throw new JsonResponseException(shopOrderRes.getError());
        }

        return Boolean.TRUE;

    }


    /**
     * 订单信息和收货地址信息封装 for 新建售后订单展示订单信息
     * @param id 交易订单id
     * @return 订单信息和收货地址信息封装DTO
     */
    @RequestMapping(value = "/api/order/{id}/for/after/sale", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<ShopOrderWithReceiveInfo> afterSaleOrderInfo(@PathVariable("id") Long id) {


        Response<ShopOrder> shopOrderRes = shopOrderReadService.findById(id);
        if(!shopOrderRes.isSuccess()){
            log.error("find shop order by id:{} fail,error:{}",id,shopOrderRes.getError());
            return Response.fail(shopOrderRes.getError());
        }
        ShopOrder shopOrder = shopOrderRes.getResult();

        Response<List<ReceiverInfo>> response = receiverInfoReadService.findByOrderId(id, OrderLevel.SHOP);
        if(!response.isSuccess()){
            log.error("find order receive info by order id:{} fial,error:{}",id,response.getError());
            return Response.fail(response.getError());
        }
        List<ReceiverInfo> receiverInfos = response.getResult();
        if(CollectionUtils.isEmpty(receiverInfos)){
            log.error("not find receive info by order id:{}",id);
            return Response.fail("order.receive.info.not.exist");
        }

        ShopOrderWithReceiveInfo withReceiveInfo = new ShopOrderWithReceiveInfo();
        withReceiveInfo.setShopOrder(shopOrder);
        withReceiveInfo.setReceiverInfo(receiverInfos.get(0));

        return Response.ok(withReceiveInfo);
    }


}