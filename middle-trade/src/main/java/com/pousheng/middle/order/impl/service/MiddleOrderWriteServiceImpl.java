package com.pousheng.middle.order.impl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.pousheng.middle.order.impl.dao.InvoiceExtDao;
import com.pousheng.middle.order.impl.dao.SkuOrderExtDao;
import com.pousheng.middle.order.impl.manager.MiddleOrderManager;
import com.pousheng.middle.order.model.InvoiceExt;
import com.pousheng.middle.order.model.SkuOrderExt;
import com.pousheng.middle.order.service.MiddleOrderWriteService;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.impl.dao.InvoiceDao;
import io.terminus.parana.order.impl.dao.OrderInvoiceDao;
import io.terminus.parana.order.impl.dao.OrderReceiverInfoDao;
import io.terminus.parana.order.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by tony on 2017/7/21.
 * pousheng-middle
 */
@Slf4j
@Service
public class MiddleOrderWriteServiceImpl implements MiddleOrderWriteService{
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

    private static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();
    private static final JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();

    @Override
    public Response<Boolean> updateOrderStatusAndSkuQuantities(ShopOrder shopOrder,List<SkuOrder> skuOrders, OrderOperation operation) {
        try{
            //更新订单状态逻辑,带事物
            middleOrderManager.updateOrderStatusAndSkuQuantities(shopOrder,skuOrders,operation);
            return Response.ok();

        }catch (ServiceException e1){
            log.error("failed to update order.cause:{}",Throwables.getStackTraceAsString(e1));
            return Response.fail(e1.getMessage());
        } catch (Exception e){
            log.error("failed to update order, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("order.update.fail");
        }
    }

    @Override
    public Response<Boolean> updateOrderStatusAndSkuQuantitiesForSku(ShopOrder shopOrder, List<SkuOrder> skuOrders, SkuOrder skuOrder, OrderOperation cancelOperation, OrderOperation waitHandleOperation,String skuCode) {
        try{
            //更新订单状态逻辑,带事物
            middleOrderManager.updateOrderStatusAndSkuQuantitiesForSku(shopOrder,skuOrders,skuOrder,cancelOperation,waitHandleOperation,skuCode);
            return Response.ok();

        }catch (ServiceException e1){
            log.error("failed to update order.cause:{}",Throwables.getStackTraceAsString(e1));
            return Response.fail(e1.getMessage());
        } catch (Exception e){
            log.error("failed to update order, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("order.update.fail");
        }
    }

    @Override
    public Response<Boolean> updateSkuOrderCodeAndSkuId(long skuId, String skuCode, long id) {
        try{
            SkuOrderExt skuOrderExt = new SkuOrderExt();
            skuOrderExt.setId(id);
            skuOrderExt.setSkuId(skuId);
            skuOrderExt.setSkuCode(skuCode);
            boolean result = skuOrderExtDao.updateSkuCodeAndSkuIdById(skuOrderExt);
            if (!result){
                log.error("failed to update skuOrder(id={}),skuId is({}),skuCode is({})",id,skuId,skuCode);
                return Response.fail("skuOrder.update.fail");
            }
            return Response.ok();
        }catch (Exception e){
            log.error("failed to update skuOrder(id={}),skuId is({}),skuCode is({}),cause by {}",id,skuId,skuCode,e.getCause());
            return Response.fail("skuOrder.update.fail");
        }
    }

    @Override
    public Response<Boolean> updateReceiveInfos(long shopOrderId, Map<String,String> receiverInfoMap,String buyerNote) {
        try{
            List<OrderReceiverInfo> receiverInfos = orderReceiverInfoDao.findByOrderIdAndOrderLevel(shopOrderId, OrderLevel.SHOP);
            OrderReceiverInfo orderReceiverInfo = receiverInfos.get(0);
            //获取初始的receviceInfo的json信息
            String receiverInfoJson = orderReceiverInfo.getReceiverInfoJson();
            //将初始的json转成map
            Map<String, String> originReceiverMap = JSON_MAPPER.fromJson(receiverInfoJson, JSON_MAPPER.createCollectionType(HashMap.class, String.class, String.class));
            //合并传入的map信息
            originReceiverMap.putAll(receiverInfoMap);
            //将合并的map转化为json
            orderReceiverInfo.setReceiverInfoJson(objectMapper.writeValueAsString(originReceiverMap));
            //在一个事务中更新收货信息,买家备注
            middleOrderManager.updateReceiverInfoAndBuyerNote(shopOrderId,orderReceiverInfo,buyerNote);

            return Response.ok();
        }catch (ServiceException e){
            log.error("failed to update orderReceiveInfo failed,(shopOrderId={})),buyerNote(={})",shopOrderId,buyerNote);
            return Response.fail(e.getMessage());
        }
        catch (Exception e){
            log.error("failed to update orderReceiveInfo failed,(shopOrderId={})),buyerNote(={})",shopOrderId,buyerNote);
            return Response.fail("receiveInfo.update.fail");
        }
    }

    @Override
    public Response<Boolean> updateInvoices(long shopOrderId, Map<String, String> invoicesMap) {
        try {
            List<OrderInvoice> orderInvoices = this.orderInvoiceDao.findByOrderIdAndOrderType(shopOrderId, Integer.valueOf(OrderLevel.SHOP.getValue()));
            OrderInvoice orderInvoice = orderInvoices.get(0);
            Invoice invoice = invoiceDao.findById(orderInvoice.getId());
            Map<String, String> originInvoiceMap = invoice.getDetail();
            originInvoiceMap.putAll(invoicesMap);
            //更新操作
            InvoiceExt invoiceExt = new InvoiceExt();
            invoiceExt.setId(invoice.getId());
            invoiceExt.setDetail(originInvoiceMap);
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

}
