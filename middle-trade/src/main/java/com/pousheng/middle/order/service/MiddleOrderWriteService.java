package com.pousheng.middle.order.service;

import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.*;
import io.terminus.parana.spu.model.SkuTemplate;

import java.util.List;
import java.util.Map;

/**
 * Created by tony on 2017/7/21.
 * pousheng-middle
 */
public interface MiddleOrderWriteService {

    /**
     * 更新总单与子单的状态(事物操作)以及回滚子单的待处理数量,整单取消使用
     *
     * @param skuOrders
     * @param operation
     */
    public Response<Boolean> updateOrderStatusAndSkuQuantities(ShopOrder shopOrder, List<SkuOrder> skuOrders, OrderOperation operation);



    /**
     * 更新总单与子单的状态(事物操作) for jit
     *
     * @param shopOrder
     * @param operation
     */
    public Response<Boolean> updateOrderStatusForJit(ShopOrder shopOrder,  OrderOperation operation);


    /**
     * 更新订单状态,回滚子单待处理数量,子单取消使用
     *
     * @param shopOrder       店铺订单
     * @param skuOrders       需要回滚成待处理状态的子单
     * @param cancelList        需要撤销的子单
     * @param cancelOperation 撤销子单取消动作,取消成功或者取消失败
     * @param skuCode         子单撤单失败时添加的skuCode用于标识
     * @return
     */
    public Response<Boolean> updateOrderStatusAndSkuQuantitiesForSku(ShopOrder shopOrder, List<SkuOrder> skuOrders, List<SkuOrder> cancelList, OrderOperation cancelOperation, OrderOperation waitHandleOperation, String skuCode);

    /**
     * 更新子单sku信息
     * @param skuTemplate
     * @param id
     * @return
     */
    Response<Boolean> updateSkuInfo(SkuTemplate skuTemplate, long id) ;

    /**
     * 更新订单的收货信息
     *
     * @param shopOrderId     店铺订单主键
     * @param receiverInfoMap 编辑的收货信息
     * @param buyerNote       买家备注
     * @return
     */
    public Response<Boolean> updateReceiveInfos(long shopOrderId, Map<String, Object> receiverInfoMap, String buyerNote);

    /**
     * 编辑订单的发票信息
     *
     * @param shopOrderId 店铺订单主键
     * @param invoicesMap 编辑的发票信息
     * @return
     */
    public Response<Boolean> updateInvoices(long shopOrderId, Map<String, String> invoicesMap, String title);

    /**
     * 插入 orderInvoice
     * @param orderInvoice
     * @return
     */
    Response<Boolean> createOrderInvoice(OrderInvoice orderInvoice);
    /**
     * 更新订单的售后地址信息
     *
     * @param shopOrderId  店铺订单id
     * @param receiverInfo 新的收货地址信息
     * @return 是否更新成功
     */
    Response<Boolean> updateReceiveInfo(Long shopOrderId, ReceiverInfo receiverInfo);

    /**
     * 更新订单的买家信息
     *
     * @param shopOrderId 店铺订单id
     * @param buyerName   新的买家名称
     * @param outBuyerId  宝胜项目用作手机号
     * @return 是否更新成功
     */
    Response<Boolean> updateBuyerInfoOfOrder(Long shopOrderId, String buyerName, String outBuyerId);

    /**
     * 修改手机号
     *
     * @param shopOrderId 店铺订单主键
     * @param outBuyerId  手机号
     * @return
     */
    Response<Boolean> updateMobileByShopOrderId(Long shopOrderId, String outBuyerId);


    /**
     * 更新店铺订单
     *
     * @param shopOrder
     * @return
     */
    Response<Boolean> updateShopOrder(ShopOrder shopOrder);


    /**
     * 更新子订单
     *
     * @param skuOrder
     * @return
     */
    Response<Boolean> updateSkuOrder(SkuOrder skuOrder);

    /**
     * @param id                 订单号
     * @param newHandleStatus    新的handleStatus
     * @param originHandleStatus 旧的handleStatus
     * @return
     */
    Response<Boolean> updateHandleStatus(Long id, String newHandleStatus, String originHandleStatus);

}
