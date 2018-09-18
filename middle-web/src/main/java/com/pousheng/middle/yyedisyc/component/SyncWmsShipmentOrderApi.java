package com.pousheng.middle.yyedisyc.component;

import com.github.kevinsawicki.http.HttpRequest;
import com.pousheng.middle.hksyc.utils.Numbers;
import com.pousheng.middle.yyedisyc.dto.trade.WmsShipmentInfo;
import com.pousheng.middle.yyedisyc.dto.trade.WmsShipmentInfoRequest;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author tanlongjun
 */
@Component
@Slf4j
public class SyncWmsShipmentOrderApi {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private static final String SID = "PS_ERP_WMS_wmsjitdeliver";

    @Value("${gateway.yyedi.host}")
    private String hkGateway;

    @Value("${gateway.yyedi.accessKey}")
    private String accessKey;

    public String doSyncShipmentOrder(WmsShipmentInfo requestData) {

        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);

        WmsShipmentInfoRequest request = new WmsShipmentInfoRequest();
        request.setSid(SID);
        String now = DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN));
        request.setTranReqDate(now);
        request.setBizContent(requestData);
        String paramJson = JsonMapper.nonEmptyMapper().toJson(request);
        log.info("sync shipment to wms erp paramJson:{}", paramJson);
        String gateway = hkGateway + "/common/pserp/wms/wmsjitdeliver";
        String responseBody = HttpRequest.post(gateway)
            .header("verifycode", accessKey)
            .header("serialNo", serialNo)
            .header("sendTime", now)
            .contentType("application/json")
            //.trustAllHosts().trustAllCerts()
            .send(paramJson)
            .connectTimeout(10000).readTimeout(10000)
            .body();

        log.info("sync shipment to wms erp result:{}", responseBody);
        return responseBody;
    }
}