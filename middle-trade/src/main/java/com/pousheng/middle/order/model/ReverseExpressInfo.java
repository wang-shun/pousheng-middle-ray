package com.pousheng.middle.order.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.constants.JacksonType;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * Code generated by terminus code gen
 * Desc: Model类
 * Date: 2019-06-03
 */
@Data
public class ReverseExpressInfo implements Serializable {

    protected static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();

    private Long id;
    
    /**
     * 交接单号
     */
    private String transferOrderId;
    
    /**
     * 承运商编码
     */
    private String carrierCode;
    
    /**
     * 承运方名称
     */
    private String carrierName;
    
    /**
     * 支付类型（支付前，货到付款）
     */
    private Integer paidAfterDelivery;
    
    /**
     * 行号
     */
    private String lineNo;
    
    /**
     * 快递单号
     */
    private String expressNo;
    
    /**
     * 寄件人姓名
     */
    private String senderName;
    
    /**
     * 寄件人电话
     */
    private String senderMobile;
    
    /**
     * 是否有单
     */
    private Integer hasOrder;
    
    /**
     * 入库单号
     */
    private String instoreNo;
    
    /**
     * 店铺
     */
    private String shop;
    
    /**
     * 渠道
     */
    private String channel;
    
    /**
     * 备注
     */
    private String buyerMemo;
    
    private Date createdAt;
    
    private Date updatedAt;

    /**
     * 外部单号创建时间
     */
    private Date outCreatedAt;
    
    private String createdBy;
    
    private String updatedBy;

    /**
     * 状态
     */
    private String status;
    
    /**
     * 扩展字段
     */
    private String extraJson;

    /**
     * 额外信息,不持久化到数据库
     */
    protected Map<String, String> extra;


    public void setExtraJson(String extraJson) throws Exception {
        this.extraJson = extraJson;
        if (Strings.isNullOrEmpty(extraJson)) {
            this.extra = Collections.emptyMap();
        } else {
            this.extra = objectMapper.readValue(extraJson, JacksonType.MAP_OF_STRING);
        }
    }

    public void setExtra(Map<String, String> extra) {
        this.extra = extra;
        if (extra == null || extra.isEmpty()) {
            this.extraJson = null;
        } else {
            try {
                this.extraJson = objectMapper.writeValueAsString(extra);
            } catch (Exception e) {
                //ignore this fuck exception
            }
        }
    }
}
