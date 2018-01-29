package com.pousheng.middle.order.constant;

/**
 * 交易模块相关常量
 * Created by songrenfei on 17/3/17
 */
public class TradeConstants {

    //待处理商品数量
    public static final String WAIT_HANDLE_NUMBER = "waitHandleNumber";

    //发货仓ID
    public static final String WAREHOUSE_ID = "warehouseId";

    //发货仓名称
    public static final String WAREHOUSE_NAME = "warehouseName";

    //绩效店铺名称
    public static final String ERP_PERFORMANCE_SHOP_NAME = "erpPerformanceShopName";
    //绩效店铺编码
    public static final String ERP_PERFORMANCE_SHOP_CODE = "erpPerformanceShopCode";
    //下单店铺名称
    public static final String ERP_ORDER_SHOP_NAME = "erpOrderShopName";
    //下单店铺编码
    public static final String ERP_ORDER_SHOP_CODE = "erpOrderShopCode";


    //发货单商品信息
    public static final String SHIPMENT_ITEM_INFO = "shipmentItemInfo";
    //发货单扩展信息
    public static final String SHIPMENT_EXTRA_INFO = "shipmentExtraInfo";


    //逆向单商品信息
    public static final String REFUND_ITEM_INFO = "refundItemInfo";
    //逆向单换货商品信息
    public static final String REFUND_CHANGE_ITEM_INFO = "refundChangeItemInfo";
    //逆向单丢件补发商品信息
    public static final String REFUND_LOST_ITEM_INFO="refundLostItemInfo";
    //逆向单商品信息
    public static final String REFUND_EXTRA_INFO = "refundExtraInfo";
    //通知电商平台状态
    public static final String ECP_ORDER_STATUS = "ecpOrderStatus";
    //冗余的shipmentId
    public static final String ECP_SHIPMENT_ID ="ecpShipmentId";
    //从电商平台拉取消子单时,子单取消失败,需要将取消失败的子单冗余进总单的extra
    public static final String SKU_CODE_CANCELED="skuCodeCanceled";
    //同步恒康响应头
    public static final String HK_RESPONSE_HEAD ="head";
    //同步恒康发货单返回的body
    public static final String HK_SHIPMENT_ORDER_BODY="orderBody";
    //同步恒康售后单返回的body
    public static final String SYNC_HK_REFUND_BODY="refundBody";
    //发票类型
    public static final String INVOICE_TYPE="type";
    //发票抬头类型
    public static final String INVOICE_TITLE_TYPE="titleType";
    //sku商品初始价格
    public static final String SKU_PRICE="skuPrice";
    //积分
    public static final String SKU_INTEGRAL="integral";
    //客服备注
    public static final String CUSTOMER_SERVICE_NOTE="customerServiceNote";
    //恒康售后单id
    public static final String HK_REFUND_ID="hkRefundId";
    //京东物流编码
    public static final String JD_VEND_CUST_ID="JDCOD";
    //自选物流编码
    public static final String OPTIONAL_VEND_CUST_ID="ZX000001";
    //子单分拆优惠
    public static final String SKU_SHARE_DISCOUNT="shareDiscount";
    //订单支付信息
    public static final String ORDER_PAYMENT_INFO="paymentInfo";
    //外部电商商品id
    public static final String MIDDLE_OUT_ITEM_ID="outItemId";
    //判断售后单是否完善的标记位(有这个标记位则说明可以自动审核,默认0)
    public static final String MIDDLE_REFUND_COMPLETE_FLAG="refundCompleteFlag";
    //hk绩效店铺名称
    public static final String HK_PERFORMANCE_SHOP_NAME="hkPerformanceShopName";
    //hk绩效店铺代码
    public static final String HK_PERFORMANCE_SHOP_CODE="hkPerformanceShopCode";
    //hk绩效店铺外码
    public static final String HK_PERFORMANCE_SHOP_OUT_CODE="hkPerformanceShopOutCode";
    //中台换货收货地址
    public static final String MIDDLE_CHANGE_RECEIVE_INFO="middleChangeReceiveInfo";
    //默认退货仓id
    public static final String DEFAULT_REFUND_WAREHOUSE_ID="defaultReWarehouseId";
    //默认退货仓名称
    public static final String DEFAULT_REFUND_WAREHOUSE_NAME="defaultReWarehouseName";
    //默认退货仓对应的外码
    public static final String DEFAULT_REFUND_OUT_WAREHOUSE_CODE="defaultReWarehouseCode";
    //公司代码(账套)
    public static final String HK_COMPANY_CODE="companyCode";
    //不自动生成发货单的备注
    public static final String NOT_AUTO_CREATE_SHIPMENT_NOTE="shipmentNote";
    /**
     * 逆向单来源
     * see RefundSource
     */
    public static final String REFUND_SOURCE = "refundSource";
    /**
     * 是否是预售订单
     */
    public static final String IS_STEP_ORDER="isStepOrder";
    /**
     * 预售订单状态 1.付完定金没有付尾款,2.付完定金和尾款
     */
    public static final String STEP_ORDER_STATUS="stepOrderStatus";


    /**
     * 售中逆向单 取消 处理结果
     * 0 待取消 1已取消
     */
    public static final String REFUND_WAIT_CANCEL = "0";
    public static final String REFUND_CANCELED = "1";

    //活动商品
    public static final String ACTIVITY_ITEM="activityItems";
    //赠品
    public static final String GIFT_ITEM="giftItems";

    public static final String ACTIVITY_SHOP="activityShops";

    /**
     * mpos 状态
     */
    //发货单接单,待发货
    public static final String MPOS_SHIPMENT_WAIT_SHIP = "1";
    //发货单拒单
    public static final String MPOS_SHIPMENT_REJECT = "-1";
    //发货单发货,待收货
    public static final String MPOS_SHIPMENT_SHIPPED = "3";
    //店铺发货
    public static final String MPOS_SHOP_DELIVER = "1";
    //仓库发货
    public static final String MPOS_WAREHOUSE_DELIVER = "2";

    //活动赠品id
    public static final String  GIFT_ACTIVITY_ID= "giftActivityId";
    //活动赠品名称
    public static final String  GIFT_ACTIVITY_NAME="giftActivityName";
    //sku订单中的shipmentId
    public static final String SKU_ORDER_SHIPMENT_ID="skuShipmentId";
    //店铺订单选择的快递单号
    public static final String SHOP_ORDER_HK_EXPRESS_CODE="orderHkExpressCode";
    public static final String SHOP_ORDER_HK_EXPRESS_NAME="orderHkExpressName";
    public static final String SKU_ORDER_CANCEL_REASON="skuOrderCancelReason";
    public static final String SHOP_ORDER_CANCEL_REASON="shopOrderCancelReason";
    //是否指定门店 1 指定 2 未指定
    public static final String IS_ASSIGN_SHOP = "isAssignShop";
    //指定门店id
    public static final String ASSIGN_SHOP_ID = "assignShopId";
    //1 门店发货 2 门店自提
    public static final String IS_SINCE = "isSince";
    //订单取消
    public static final String ORDER_CANCEL = "order";
    //发货单取消
    public static final String SHIPMENT_CANCEL = "shipment";
    //仓库安全库存
    public static final String WAREHOUSE_SAFESTOCK = "safeStock";
    //仓库虚拟店编码
    public static final String WAREHOUSE_VIRTUALSHOPCODE = "virtualShopCode";
    //仓库虚拟店名称
    public static final String WAREHOUSE_VIRTUALSHOPNAME = "virtualShopName";
    //仓库退货仓id
    public static final String WAREHOUSE_RETURNWAREHOUSEID = "returnWarehouseId";
    //仓库退货仓编码
    public static final String WAREHOUSE_RETURNWAREHOUSECODE = "returnWarehouseCode";
    //仓库退货仓名称
    public static final String WAREHOUSE_RETURNWAREHOUSENAME = "returnWarehouseName";
    //mpos接单员工
    public static final String MPOS_RECEIVE_STAFF = "mposReceiceStaff";
    //mpos拒绝原因
    public static final String MPOS_REJECT_REASON = "mposRejectReason";
    //快递单号
    public static final String SHIP_SERIALNO = "shipmentSerialNo";
    //快递代码
    public static final String SHIP_CORP_CODE = "shipmentCorpCode";
    //发货时间
    public static final String SHIP_DATE = "shipmentDate";
    //同步无法派单产品失败
    public static final Integer FAIL_NOT_DISPATCHER_SKU_TO_MPOS = 1;
    //同步退货单收货失败
    public static final Integer FAIL_REFUND_RECEIVE_TO_MPOS = 2;
    //同步发货单pos信息给恒康失败
    public static final Integer FAIL_SYNC_POS_TO_HK = 3;
    //同步发货单收货给恒康失败
    public static final Integer FAIL_SYNC_SHIPMENT_CONFIRM_TO_HK = 4;
    //同步退货单给恒康失败
    public static final Integer FAIL_SYNC_REFUND_TO_HK = 5;

    public static final String SKU_CANNOT_BE_DISPATCHED = "该商品无法派出";


}
