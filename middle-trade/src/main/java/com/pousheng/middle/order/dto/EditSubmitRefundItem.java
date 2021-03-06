package com.pousheng.middle.order.dto;

import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/12/13
 * pousheng-middle
 */
@Data
public class EditSubmitRefundItem implements java.io.Serializable {
    private static final long serialVersionUID = 5366346151064699012L;

    private Long skuOrderId;
    /**
     *  商品编码和数量 (退货)
     */
    private String refundSkuCode;

    /**
     * 商品外部 ID，商品编码条码可能会重复，用 ID 区分
     */
    private String  refundOutSkuCode;

    /**
     *  数量 (退货)
     */
    private Integer refundQuantity;
    //退货商品对应的换货商品集合

    /**
     *  退款金额
     */
    private Long fee;

    /**
     * 子售后单id
     */
    private String skuAfterSaleId;

    /**
     * 商品名称
     */
    private String itemName;
}
