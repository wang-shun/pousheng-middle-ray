package com.pousheng.middle.order.service;

import com.pousheng.middle.order.dto.OrderShipmentCriteria;
import com.pousheng.middle.order.dto.ShipmentPagingInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.OrderShipment;

import java.util.List;

/**
 * Created by songrenfei on 2017/6/20
 */
public interface OrderShipmentReadService {


    /**
     * 根据(子)订单id和级别查找对应的发货单列表
     *
     * @param orderId    (子)订单id
     * @param orderLevel 级别(宝胜统一为 shop) {@link OrderLevel}
     * @return 对应的发货单列表
     */
    Response<List<OrderShipment>> findByOrderIdAndOrderLevel(Long orderId, OrderLevel orderLevel);


    /**
     * 根据(子)售后单id和级别查找对应的发货单列表
     *
     * @param afterSaleOrderId    (子)售后单id
     * @param orderLevel 级别(宝胜统一为 shop) {@link OrderLevel}
     * @return 对应的发货单列表
     */
    Response<List<OrderShipment>> findByAfterSaleOrderIdAndOrderLevel(Long afterSaleOrderId, OrderLevel orderLevel);

    /**
     * 发货单分页列表, 供开放平台使用
     *
     * @param criteria 发货查询条件
     * @return 分页发货单封装信息
     */
    Response<Paging<ShipmentPagingInfo>> findBy(OrderShipmentCriteria criteria);

    /**
     * 根据主键id查询发货单关联信息
     * @param id 主键id
     * @return 发货单
     */
    Response<OrderShipment> findById(Long id);


    /**
     * 根据主键id查询发货单关联信息
     * @param shipmentId 主键id
     * @return 发货单
     */
    Response<OrderShipment> findByShipmentId(Long shipmentId);

    /**
     * 根据订单id 商品编码 可退货数量查询发货单关联信息 for 拉取第三方渠道的逆向订单时需要判断是否需要人工拆单（退货、换货）
     * @param id 订单id
     * @param skuCode 商品编码
     * @param quantity 退货数量
     * @return 发货单
     */
    Response<OrderShipment> findByOrderIdAndSkuCodeAndQuantity(Long id,String skuCode,Integer quantity);

    /**
     * 分页获取orderShipment 修复数据用
     * @param criteria 参数
     * @return 订单发货单关联关系
     */
    Response<Paging<OrderShipment>> paging(OrderShipmentCriteria criteria);


    /**
     * 统计今日指派给店铺的发货单数量
     * @param shopId
     * @return
     */
    Response<Integer>  countByShopId(Long shopId);
}
