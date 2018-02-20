package com.pousheng.middle.web.order.sync.mpos;

import io.terminus.open.client.parana.component.ParanaClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * created by penghui on 2017/12/30
 * 同步mpos
 */
@Component
@Slf4j
public class SyncMposApi {

    @Autowired
    private ParanaClient paranaClient;

    @Value("${mpos.open.shop.id:180}")
    private Long shopId;

    /**
     * 同步发货单到mpos
     * @param param 参数
     * @return
     */
    public String syncShipmentToMpos(Map<String,Object> param){
        log.info("sync shipments to mpos,param:{}",param);
        String responseBody = paranaClient.post(shopId,"mpos.order.ship.api",param);
        log.info("response:{}",responseBody);
        return responseBody;
    }

    /**
     * 同步全渠道订单的发货单到mpos
     * @param param
     * @return
     */
    public String syncAllChannelShipmnetToMpos(Map<String,Object> param){
        log.info("sync all-channel-order shipments to mpos,param:{}",param);
        String responseBody = paranaClient.post(shopId,"sync.order.ship.api",param);
        log.info("response:{}",responseBody);
        return responseBody;
    }
    /**
     * 只有恒康发货才通知mpos状态更新
     * @param param 参数
     * @return
     */
    public String syncShipmentShippedToMpos(Map<String,Object> param){
        log.info("sync shipment shipped to mpos,param:{}",param);
        String responseBody = paranaClient.post("mpos.order.ship.express",param);
        log.info("response:{}",responseBody);
        return responseBody;
    }

    /**
     * 商品派不出去，同步mpos
     * @param param 参数
     * @return
     */
    public String syncNotDispatcherSkuToMpos(Map<String,Object> param){
        log.info("sync not dispatcher sku to mpos,param:{}",param);
        String responseBody = paranaClient.post(shopId,"mpos.reject.afterSales",param);
        log.info("response:{}",responseBody);
        return responseBody;
    }

    /**
     * 拉取mpos发货单状态
     * @param param
     * @return
     */
    public String syncShipmentStatus(Map<String,Object> param){
        log.info("sync shipments status from mpos,param:{}",param);
        String responseBody = paranaClient.get(shopId,"mpos.query.ship.status",param);
        log.info("response:{}",responseBody);
        return responseBody;
    }

    /**
     * 恒康收到退货后，通知mpos退款
     * @param param
     * @return
     */
    public String syncRefundReceive(Map<String,Object> param){
        log.info("sync shipments status from mpos,param:{}",param);
        String responseBody = paranaClient.post(shopId,"mpos.seller.confirm.afterSales",param);
        log.info("response:{}",responseBody);
        return responseBody;
    }

    /**
     * 如果是电发的发货单，在未发货之前通知mpos取消发货单
     * @param param 通知取消发货单的参数集合
     * @return 返回的取消结果
     */
    public String syncMposToCancelShipment(Map<String,Object> param){
        log.info("sync mpos to cancel shipment,param:{}",param);
        String responseBody = paranaClient.post(shopId,"",param);
        log.info("response:{}",responseBody);
        return responseBody;
    }
}