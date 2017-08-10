package com.pousheng.middle.open;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.EcpOrderStatus;
import com.pousheng.middle.spu.service.PoushengMiddleSpuService;
import com.taobao.api.domain.Trade;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.open.client.center.job.order.component.DefaultOrderReceiver;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.order.dto.OpenClientFullOrder;
import io.terminus.open.client.order.enums.OpenClientOrderStatus;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.order.dto.RichOrder;
import io.terminus.parana.order.dto.RichSku;
import io.terminus.parana.order.dto.RichSkusByShop;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.service.SpuReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by cp on 7/25/17.
 */
@Component
@Slf4j
public class PsOrderReceiver extends DefaultOrderReceiver {

    @RpcConsumer
    private SpuReadService spuReadService;

    @RpcConsumer
    private PoushengMiddleSpuService middleSpuService;

    @RpcConsumer
    private OrderWriteService orderWriteService;

    @Override
    protected Item findItemById(Long paranaItemId) {
        //TODO use cache

        Response<Spu> findR = spuReadService.findById(paranaItemId);
        if (!findR.isSuccess()) {
            log.error("fail to find spu by id={},cause:{}", paranaItemId, findR.getError());
            return null;
        }
        Spu spu = findR.getResult();

        Item item = new Item();
        item.setId(spu.getId());
        item.setName(spu.getName());
        item.setMainImage(spu.getMainImage_());
        return item;
    }

    @Override
    protected Sku findSkuByCode(String skuCode) {
        //TODO use cache
        Response<SkuTemplate> findR = middleSpuService.findBySkuCode(skuCode);
        if (!findR.isSuccess()) {
            log.error("fail to find sku template by code={},cause:{}",
                    skuCode, findR.getError());
            return null;
        }
        SkuTemplate skuTemplate = findR.getResult();

        Sku sku = new Sku();
        sku.setId(skuTemplate.getId());
        sku.setName(skuTemplate.getName());
        sku.setPrice(skuTemplate.getPrice());
        sku.setSkuCode(skuTemplate.getSkuCode());
        try {
            sku.setExtraPrice(skuTemplate.getExtraPrice());
        } catch (Exception e) {
            //ignore
        }
        sku.setImage(skuTemplate.getImage_());
        sku.setAttrs(skuTemplate.getAttrs());
        return sku;
    }

    protected void updateParanaOrder(ShopOrder shopOrder, OpenClientFullOrder openClientFullOrder) {
        if (openClientFullOrder.getStatus() == OpenClientOrderStatus.CONFIRMED) {
            Response<Boolean> updateR = orderWriteService.shopOrderStatusChanged(shopOrder.getId(),
                    shopOrder.getStatus(), MiddleOrderStatus.CONFIRMED.getValue());
            if (!updateR.isSuccess()) {
                log.error("failed to change shopOrder(id={})'s status from {} to {} when sync order, cause:{}",
                        shopOrder.getId(), shopOrder.getStatus(), MiddleOrderStatus.CONFIRMED.getValue(), updateR.getError());
            }
        }
    }


    protected RichOrder makeParanaOrder(OpenClientShop openClientShop,
                                        OpenClientFullOrder openClientFullOrder) {
        RichOrder richOrder = super.makeParanaOrder(openClientShop,openClientFullOrder);
        //初始化店铺订单的extra
        RichSkusByShop richSkusByShop = richOrder.getRichSkusByShops().get(0);
        Map<String,String> shopOrderExtra = richSkusByShop.getExtra();
        shopOrderExtra.put(TradeConstants.ECP_ORDER_STATUS,String.valueOf(EcpOrderStatus.WAIT_SHIP.getValue()));
        richSkusByShop.setExtra(shopOrderExtra);
        //初始化店铺子单extra
        List<RichSku> richSkus = richSkusByShop.getRichSkus();
        richSkus.forEach(richSku -> {
            Map<String,String> skuExtra = richSku.getExtra();
            skuExtra.put(TradeConstants.WAIT_HANDLE_NUMBER,String.valueOf(richSku.getQuantity()));
            richSku.setExtra(skuExtra);
        });
        return richOrder;
    }
}
