package com.pousheng.middle.order.impl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.pousheng.middle.order.impl.dao.InvoiceExtDao;
import com.pousheng.middle.order.impl.dao.ShopOrderExtDao;
import com.pousheng.middle.order.impl.dao.SkuOrderExtDao;
import com.pousheng.middle.order.impl.manager.MiddleOrderManager;
import com.pousheng.middle.order.model.InvoiceExt;
import com.pousheng.middle.order.model.ShopOrderExt;
import com.pousheng.middle.order.model.SkuOrderExt;
import com.pousheng.middle.order.service.MiddleOrderWriteService;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.impl.dao.*;
import io.terminus.parana.order.model.*;
import io.terminus.parana.spu.model.SkuTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by tony on 2017/7/21.
 * pousheng-middle
 */
@Slf4j
@Service
public class MiddleOrderWriteServiceImpl implements MiddleOrderWriteService {
    @Autowired
    private MiddleOrderManager middleOrderManager;
    @Autowired
    private SkuOrderExtDao skuOrderExtDao;
    @Autowired
    private OrderReceiverInfoDao orderReceiverInfoDao;
    @Autowired
    private OrderInvoiceDao orderInvoiceDao;
    @Autowired
    private InvoiceDao invoiceDao;
    @Autowired
    private InvoiceExtDao invoiceExtDao;
    @Autowired
    private ShopOrderExtDao shopOrderExtDao;
    @Autowired
    private ShopOrderDao shopOrderDao;
    @Autowired
    private SkuOrderDao skuOrderDao;

    private static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();
    private static final JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();

    @Override
    public Response<Boolean> updateOrderStatusAndSkuQuantities(ShopOrder shopOrder, List<SkuOrder> skuOrders, OrderOperation operation) {
        try {
            //更新订单状态逻辑,带事物
            middleOrderManager.updateOrderStatusAndSkuQuantities(shopOrder, skuOrders, operation);
            return Response.ok();

        } catch (ServiceException e) {
            log.error("failed to update order(id:{}) operation:{} error:{}", shopOrder.getOrderCode(), operation.getText(), e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("failed to update order(id:{}) operation:{}, cause:{}", shopOrder.getOrderCode(), operation.getText(), Throwables.getStackTraceAsString(e));
            return Response.fail("order.update.fail");
        }
    }


    @Override
    public Response<Boolean> updateOrderStatusForJit(ShopOrder shopOrder, OrderOperation operation) {
        log.info("start update jit order status for order id:{}",shopOrder.getId());
        try {
            //更新订单状态逻辑,带事物
            middleOrderManager.updateOrderStatusForJit(shopOrder, operation);
            return Response.ok();

        } catch (ServiceException e) {
            log.error("failed to update order(id:{}) operation:{} error:{}", shopOrder.getOrderCode(), operation.getText(), e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("failed to update order(id:{}) operation:{}, cause:{}", shopOrder.getOrderCode(), operation.getText(), Throwables.getStackTraceAsString(e));
            return Response.fail("order.update.fail");
        }
    }

    @Override
    public Response<Boolean> updateOrderStatusAndSkuQuantitiesForSku(ShopOrder shopOrder, List<SkuOrder> skuOrders, List<SkuOrder> cancelList, OrderOperation cancelOperation, OrderOperation waitHandleOperation, String skuCode) {
        try {
            //更新订单状态逻辑,带事物
            middleOrderManager.updateOrderStatusAndSkuQuantitiesForSku(shopOrder, skuOrders, cancelList, cancelOperation, waitHandleOperation, skuCode);
            return Response.ok();

        } catch (ServiceException e1) {
            log.error("failed to update order.cause:{}", Throwables.getStackTraceAsString(e1));
            return Response.fail(e1.getMessage());
        } catch (Exception e) {
            log.error("failed to update order, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("order.update.fail");
        }
    }

    @Override
    public Response<Boolean> updateSkuInfo(SkuTemplate skuTemplate, long id) {
        try {
            SkuOrderExt skuOrderExt = new SkuOrderExt();
            skuOrderExt.setId(id);
            skuOrderExt.setName(skuTemplate.getName());
            skuOrderExt.setSkuId(skuTemplate.getId());
            skuOrderExt.setSkuAttributes(skuTemplate.getAttrsJson());
            skuOrderExt.setSkuCode(skuTemplate.getSkuCode());
            boolean result = skuOrderExtDao.updateSkuInfoById(skuOrderExt);
            if (!result) {
                log.error("failed to update skuOrder(id={}),skuTemplate is({})", id, skuTemplate);
                return Response.fail("update.sku.order.failed");
            }
            return Response.ok();
        } catch (Exception e) {
            log.error("failed to update skuOrder(id={}),skuTemplate is({}),skuCode is({}),cause by {}", id, skuTemplate, Throwables.getStackTraceAsString(e));
            return Response.fail("update.sku.order.failed");
        }
    }

    @Override
    public Response<Boolean> updateReceiveInfos(long shopOrderId, Map<String, Object> receiverInfoMap, String buyerNote) {
        try {
            List<OrderReceiverInfo> receiverInfos = orderReceiverInfoDao.findByOrderIdAndOrderLevel(shopOrderId, OrderLevel.SHOP);
            OrderReceiverInfo orderReceiverInfo = receiverInfos.get(0);
            //获取初始的receviceInfo的json信息
            String receiverInfoJson = orderReceiverInfo.getReceiverInfoJson();
            //将初始的json转成map
            Map<String, Object> originReceiverMap = JSON_MAPPER.fromJson(receiverInfoJson, JSON_MAPPER.createCollectionType(HashMap.class, String.class, Object.class));
            //合并传入的map信息
            originReceiverMap.putAll(receiverInfoMap);
            //将合并的map转化为json
            orderReceiverInfo.setReceiverInfoJson(objectMapper.writeValueAsString(originReceiverMap));
            //在一个事务中更新收货信息,买家备注及手机号
            middleOrderManager.updateReceiverInfoAndBuyerNote(shopOrderId, orderReceiverInfo, buyerNote);

            return Response.ok();
        } catch (ServiceException e) {
            log.error("failed to update orderReceiveInfo failed,(shopOrderId={})),buyerNote(={})", shopOrderId, buyerNote);
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("failed to update orderReceiveInfo failed,(shopOrderId={})),buyerNote(={})", shopOrderId, buyerNote);
            return Response.fail("receiveInfo.update.fail");
        }
    }

    @Override
    public Response<Boolean> updateInvoices(long shopOrderId, Map<String, String> invoicesMap, String title) {
        try {
            List<OrderInvoice> orderInvoices = this.orderInvoiceDao.findByOrderIdAndOrderType(shopOrderId, Integer.valueOf(OrderLevel.SHOP.getValue()));
            OrderInvoice orderInvoice = orderInvoices.get(0);
            Invoice invoice = invoiceDao.findById(orderInvoice.getInvoiceId());
            Map<String, String> originInvoiceMap = invoice.getDetail();
            originInvoiceMap.putAll(invoicesMap);
            //更新操作
            InvoiceExt invoiceExt = new InvoiceExt();
            invoiceExt.setId(invoice.getId());
            invoiceExt.setDetail(originInvoiceMap);
            invoiceExt.setTitle(title);
            boolean result = invoiceExtDao.updateInvoiceDetail(invoiceExt);
            if (!result) {
                log.error("failed to update orderInvoiceInfo failed,(shopOrderId={}))", shopOrderId);
                return Response.fail("invoice.update.fail");
            }
            return Response.ok(Boolean.TRUE);

        } catch (Exception e) {
            log.error("failed to update orderInvoiceInfo failed,(shopOrderId={}))");
            return Response.fail("invoice.update.fail");
        }
    }

    @Override
    public Response<Boolean> createOrderInvoice(OrderInvoice orderInvoice) {
        return Response.ok(orderInvoiceDao.create(orderInvoice));
    }

    @Override
    public Response<Boolean> updateReceiveInfo(Long shopOrderId, ReceiverInfo receiverInfo) {
        try {
            List<OrderReceiverInfo> receiverInfos = orderReceiverInfoDao.findByOrderIdAndOrderLevel(shopOrderId, OrderLevel.SHOP);
            if (CollectionUtils.isEmpty(receiverInfos)) {
                log.error("receiver info not found where shopOrderId={}", shopOrderId);
                return Response.fail("receiver.info.not.found");
            }
            OrderReceiverInfo oldReceiverInfo = receiverInfos.get(0);

            OrderReceiverInfo orderReceiverInfo = new OrderReceiverInfo();
            orderReceiverInfo.setId(oldReceiverInfo.getId());
            orderReceiverInfo.setMobile(receiverInfo.getMobile());
            orderReceiverInfo.setReceiverInfo(receiverInfo);
            orderReceiverInfoDao.update(orderReceiverInfo);

            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("fail to update order(shopOrderId={}) receiverInfo to {},cause:{}",
                    shopOrderId, receiverInfo, Throwables.getStackTraceAsString(e));
            return Response.fail("receiver.info.update.fail");
        }
    }

    @Override
    public Response<Boolean> updateBuyerInfoOfOrder(Long shopOrderId, String buyerName, String outBuyerId) {
        try {
            ShopOrderExt shopOrderExt = new ShopOrderExt();
            shopOrderExt.setId(shopOrderId);
            shopOrderExt.setBuyerName(buyerName);
            shopOrderExt.setOutBuyerId(outBuyerId);
            shopOrderExtDao.update(shopOrderExt);

            skuOrderExtDao.updateBuyerNameByOrderId(shopOrderId, buyerName);

            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("fail to update buyerName to {} for shopOrder(id={}),cause:{}",
                    buyerName, shopOrderId, Throwables.getStackTraceAsString(e));
            return Response.fail("buyer.name.update.fail");
        }
    }

    @Override
    public Response<Boolean> updateMobileByShopOrderId(Long shopOrderId, String outBuyerId) {
        try {
            ShopOrder shopOrder = new ShopOrder();
            shopOrder.setId(shopOrderId);
            shopOrder.setOutBuyerId(outBuyerId);
            shopOrderDao.update(shopOrder);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("fail to update outBuyerId to {} for shopOrder(id={}),cause:{}",
                    outBuyerId, shopOrderId, Throwables.getStackTraceAsString(e));
            return Response.fail("buyer.name.update.fail");
        }
    }

    @Override
    public Response<Boolean> updateShopOrder(ShopOrder shopOrder) {
        try {
            shopOrderDao.update(shopOrder);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("fail to update outBuyerId to {} for shopOrder(id={}),cause:{}",
                    shopOrder.getOutId(), shopOrder.getId(), Throwables.getStackTraceAsString(e));
            return Response.fail("buyer.name.update.fail");
        }
    }

    @Override
    public Response<Boolean> updateSkuOrder(SkuOrder skuOrder) {
        try {
            skuOrderDao.update(skuOrder);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("fail to update outBuyerId to {} for shopOrder(id={}),cause:{}",
                    skuOrder.getOutId(), skuOrder.getId(), Throwables.getStackTraceAsString(e));
            return Response.fail("buyer.name.update.fail");
        }
    }

    @Override
    public Response<Boolean> updateHandleStatus(Long id, String newHandleStatus, String originHandleStatus) {
        try {
            boolean result = shopOrderExtDao.updateHandleStatus(id, newHandleStatus, originHandleStatus);
            if (result) {
                return Response.ok(Boolean.TRUE);
            } else {
                return Response.fail("new.handle.status.update.fail");
            }
        } catch (Exception e) {
            log.error("fail to update newhandleStatus to {} for shopOrder(id={}),cause:{}",
                    newHandleStatus, id, Throwables.getStackTraceAsString(e));
            return Response.fail("handle.status.update.fail");
        }
    }


}
