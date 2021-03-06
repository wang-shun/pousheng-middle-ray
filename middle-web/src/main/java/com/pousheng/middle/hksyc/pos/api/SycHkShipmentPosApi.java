package com.pousheng.middle.hksyc.pos.api;

import com.github.kevinsawicki.http.HttpRequest;
import com.pousheng.middle.hksyc.pos.dto.HkSaleRefuseRequestData;
import com.pousheng.middle.hksyc.pos.dto.HkShimentDoneRequestData;
import com.pousheng.middle.hksyc.pos.dto.HkShipmentPosRequestData;
import com.pousheng.middle.hksyc.utils.Numbers;
import com.pousheng.middle.web.biz.dto.DashBoardShipmentDTO;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by songrenfei on 2017/7/19
 */
@Component
@Slf4j
public class SycHkShipmentPosApi {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Value("${gateway.erp.host}")
    private String hkGateway;

    @Value("${gateway.erp.accessKey}")
    private String accessKey;

    public String doSyncShipmentPos(HkShipmentPosRequestData requestData,String url){
        String paramJson = JsonMapper.nonEmptyMapper().toJson(requestData);
        //log.info("paramJson:{}",paramJson);
        log.info("doSyncShipmentPos paramJson:{}",paramJson);

        return doRequest(paramJson,url);
    }

    public String doSyncRefundPos(HkShipmentPosRequestData requestData,String url){
        String paramJson = JsonMapper.nonEmptyMapper().toJson(requestData);
        //log.info("paramJson:{}",paramJson);
        log.info("doSyncRefundPos paramJson:{}",paramJson);

        return doRequest(paramJson,url);
    }

    public String doSyncSaleRefuse(HkSaleRefuseRequestData requestData,String url){
        String paramJson = JsonMapper.nonEmptyMapper().toJson(requestData);
        //log.info("paramJson:{}",paramJson);
        log.info("doSyncSaleRefuse paramJson:{}",paramJson);
        return doRequest(paramJson,url);
    }



    public String doSyncShipmentDone(HkShimentDoneRequestData requestData, String url){
        String paramJson = JsonMapper.nonEmptyMapper().toJson(requestData);
        //log.info("paramJson:{}",paramJson);
        log.info("doSyncShipmentDone paramJson:{}",paramJson);

        return doRequest(paramJson,url);
    }



    public String doSyncDashBoardShipment(DashBoardShipmentDTO requestData, String url){
        String paramJson = JsonMapper.nonEmptyMapper().toJson(requestData);
        log.info("doSyncDashBoardShipment:{}",paramJson);
        return doRequest(paramJson,url);
    }

    private String doRequest(String paramJson,String url){
        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);
        String gateway =hkGateway + url;
        String responseBody = HttpRequest.post(gateway)
                .header("verifycode",accessKey)
                .header("serialNo",serialNo)
                .header("sendTime",DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                .contentType("application/json")
                //.trustAllHosts().trustAllCerts()
                .send(paramJson)
                .connectTimeout(10000).readTimeout(10000)
                .body();

        log.info("doRequest for paramJson:{},result:{}",paramJson,responseBody);
        return responseBody;
    }
}
