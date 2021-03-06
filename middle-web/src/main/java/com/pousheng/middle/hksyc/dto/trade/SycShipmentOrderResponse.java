package com.pousheng.middle.hksyc.dto.trade;

import com.pousheng.middle.hksyc.dto.HkResponseHead;
import lombok.Data;

import java.io.Serializable;

/**
 * 同步恒康时返回信息集合,包含响应头和响应体
 * Created by tony on 2017/7/26.
 * pousheng-middle
 */
@Data
public class SycShipmentOrderResponse implements Serializable {
    private static final long serialVersionUID = -7906655550101958204L;
    private HkResponseHead head;
    private SycHkShipmentOrderResponseBody orderBody;
}
