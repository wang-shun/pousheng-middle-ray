package com.pousheng.middle.yyedisyc.component;

import com.github.kevinsawicki.http.HttpRequest;
import com.pousheng.middle.hksyc.utils.Numbers;
import com.pousheng.middle.yyedisyc.dto.trade.ParameterWMS;
import com.pousheng.middle.yyedisyc.dto.trade.YJErpRefundInfo;
import com.pousheng.middle.yyedisyc.dto.trade.YYEdiReturnInfo;
import com.pousheng.middle.yyedisyc.dto.trade.YYEdiReturnInfoBody;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.shop.OpenShopCacher;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by songrenfei on 2017/7/19
 */
@Component
@Slf4j
public class SycYYEdiRefundOrderApi {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Value("${gateway.yyedi.host}")
    private String hkGateway;

    @Value("${gateway.yyedi.accessKey}")
    private String accessKey;

    private String yjGateway;
    private String yjAccessKey;
    @Autowired
    OpenShopCacher openShopCacher;

    /**
     * 允许使用宝唯
     */
    @Value("${gateway.yyedi.bw-enable:false}")
    private boolean bwEnable;

    private final static String SID = "PS_ERP_WMS_bcrefunds";

    public String doSyncRefundOrder(YYEdiReturnInfo requestData) {

        YYEdiReturnInfoBody body = new YYEdiReturnInfoBody();
        body.bizContent(requestData).sid(SID).tranReqDate(DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)));
        String paramJson = JsonMapper.nonEmptyMapper().toJson(body);
        log.info("sync refund to yyedi erp paramJson:{} ", paramJson);
        String gateway = hkGateway + "/common/pserp/wms/pushbcrefunds";
        String responseBody = HttpRequest.post(gateway)
                .contentType("application/json")
                .header("verifycode", accessKey)
                .send(paramJson)
                .connectTimeout(10000).readTimeout(10000)
                .body();
        log.info("sync refund to yyedi erp result:{}", responseBody);
        return responseBody;
    }


    public String doSyncYJErpRefundOrder(List<YJErpRefundInfo> requestData) {

        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);
        String paramJson = JsonMapper.nonEmptyMapper().toJson(requestData.get(0));
        log.info("sync refund to yj erp paramJson:{} serialNo:{}", paramJson, serialNo);
        String gateway = hkGateway + "/common-yjerp/yjerp/default/pushmgorderexchangeset";
        if(bwEnable){
            gateway = hkGateway + "/common-yjerp/bw/yjerp/pushmgorderexchangeset";
        }
        String responseBody = HttpRequest.post(gateway)
                .header("verifycode", accessKey)
                .header("serialNo", serialNo)
                .header("sendTime", DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                .contentType("application/json")
                .send(paramJson)
                .connectTimeout(10000).readTimeout(10000)
                .body();

        log.info("sync refund to yj erp result:{}, serialNo:{}", responseBody, serialNo);
        return responseBody;
    }

    /**
     * 2019.04.16 RAY: POUS934 電商退貨單接口增加billsource參數
     *
     * @param reqData
     * @param billSource 訂單來源
     * @return responseBody
     */
    public String doSyncRefundOrder(YYEdiReturnInfo reqData, ParameterWMS.BillSource billSource) {
        reqData.setBillsource(billSource.getCode());
        return doSyncRefundOrder(reqData);
    }
}
