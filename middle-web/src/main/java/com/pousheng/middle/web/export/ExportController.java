package com.pousheng.middle.web.export;

import com.google.common.eventbus.EventBus;
import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.mq.constant.MqConstants;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.MiddleOrderCriteria;
import com.pousheng.middle.order.dto.MiddleRefundCriteria;
import com.pousheng.middle.order.dto.OrderShipmentCriteria;
import com.pousheng.middle.order.dto.PoushengSettlementPosCriteria;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.events.trade.ExportTradeBillEvent;
import com.pousheng.middle.web.utils.export.FileRecord;
import com.pousheng.middle.web.utils.permission.PermissionUtil;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by sunbo@terminus.io on 2017/7/20.
 */
@Slf4j
@RestController
@RequestMapping("api/")
public class ExportController {

    @Autowired
    private ExportService exportService;

    @Autowired
    private PermissionUtil permissionUtil;

    @Autowired
    private EventBus eventBus;

    private static final JsonMapper MAPPER = JsonMapper.JSON_NON_EMPTY_MAPPER;
    @Autowired
    private CompensateBizLogic compensateBizLogic;


    /**
     * 导出订单
     *
     * @param middleOrderCriteria 查询参数
     * @return
     */
    @GetMapping("order/export")
    public void orderExport(MiddleOrderCriteria middleOrderCriteria) {
        String criteriaStr = JsonMapper.nonEmptyMapper().toJson(middleOrderCriteria);
        if(log.isDebugEnabled()){
            log.debug("API-ORDER-EXPORT-START param: middleOrderCriteria [{}] ]",criteriaStr);
        }


        if (middleOrderCriteria.getStatus() != null && middleOrderCriteria.getStatus().contains(99)) {
            throw new JsonResponseException("this status not support export");
        }

        //获取当前用户负责的商铺id
        List<Long> currentUserCanOperatShopIds = permissionUtil.getCurrentUserCanOperateShopIDs();
        if (middleOrderCriteria.getShopId() == null) {
            middleOrderCriteria.setShopIds(currentUserCanOperatShopIds);
        } else if (!currentUserCanOperatShopIds.contains(middleOrderCriteria.getShopId())) {
            throw new JsonResponseException("permission.check.query.deny");
        }
        ExportTradeBillEvent event = new ExportTradeBillEvent();
        event.setType(TradeConstants.EXPORT_ORDER);
        event.setCriteria(middleOrderCriteria);
        event.setUserId(UserUtil.getUserId());
        //换成Biz任务形式 modified by longjun.tlj
        String context=MAPPER.toJson(event);
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.EXPORT_TRADE_BILL.name());
        biz.setContext(context);
        biz.setBizId(TradeConstants.EXPORT_ORDER);
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.name());
        compensateBizLogic.createBizAndSendMq(biz,MqConstants.POSHENG_MIDDLE_EXPORT_COMPENSATE_BIZ_TOPIC);
        if(log.isDebugEnabled()){
            log.debug("API-ORDER-EXPORT-END param: middleOrderCriteria [{}] ]",criteriaStr);
        }
    }

    /**
     * 导出售后单
     *
     * @param criteria
     */
    @GetMapping("refund/export")
    public void refundExport(MiddleRefundCriteria criteria) {
        String criteriaStr = JsonMapper.nonEmptyMapper().toJson(criteria);
        if(log.isDebugEnabled()){
            log.debug("API-REFUND-EXPORT-START param: criteria [{}] ]",criteriaStr);
        }
        //获取当前用户负责的商铺id
        List<Long> currentUserCanOperatShopIds = permissionUtil.getCurrentUserCanOperateShopIDs();
        if (criteria.getShopId() == null) {
            criteria.setShopIds(currentUserCanOperatShopIds);
        } else if (!currentUserCanOperatShopIds.contains(criteria.getShopId())) {
            throw new JsonResponseException("permission.check.query.deny");
        }
        ExportTradeBillEvent event = new ExportTradeBillEvent();
        event.setType(TradeConstants.EXPORT_REFUND);
        event.setCriteria(criteria);
        event.setUserId(UserUtil.getUserId());
        //换成Biz任务形式 modified by longjun.tlj
        String context=MAPPER.toJson(event);
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.EXPORT_TRADE_BILL.name());
        biz.setContext(context);
        biz.setBizId(TradeConstants.EXPORT_REFUND);
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.name());
        compensateBizLogic.createBizAndSendMq(biz,MqConstants.POSHENG_MIDDLE_EXPORT_COMPENSATE_BIZ_TOPIC);
        if(log.isDebugEnabled()){
            log.debug("API-REFUND-EXPORT-END param: criteria [{}] ]",criteriaStr);
        }
    }


    /**
     * 发货单导出
     */
    @GetMapping("shipment/export")
    public void shipmentExport(OrderShipmentCriteria criteria) {
        String criteriaStr = JsonMapper.nonEmptyMapper().toJson(criteria);
        if(log.isDebugEnabled()){
            log.debug("API-SHIPMENT-EXPORT-START param: criteria [{}] ]",criteriaStr);
        }
        if (criteria.getEndAt() != null) {
            criteria.setEndAt(new DateTime(criteria.getEndAt().getTime()).plusDays(1).minusSeconds(1).toDate());
        }
        //获取当前用户负责的商铺id
        List<Long> currentUserCanOperatShopIds = permissionUtil.getCurrentUserCanOperateShopIDs();
        if (criteria.getShopId() == null) {
            criteria.setShopIds(currentUserCanOperatShopIds);
        } else if (!currentUserCanOperatShopIds.contains(criteria.getShopId())) {
            throw new JsonResponseException("permission.check.query.deny");
        }
        ExportTradeBillEvent event = new ExportTradeBillEvent();
        event.setType(TradeConstants.EXPORT_SHIPMENT);
        event.setCriteria(criteria);
        event.setUserId(UserUtil.getUserId());
        //换成Biz任务形式 modified by longjun.tlj
        String context=MAPPER.toJson(event);
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.EXPORT_TRADE_BILL.name());
        biz.setContext(context);
        biz.setBizId(TradeConstants.EXPORT_SHIPMENT);
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.name());
        compensateBizLogic.createBizAndSendMq(biz,MqConstants.POSHENG_MIDDLE_EXPORT_COMPENSATE_BIZ_TOPIC);
        if(log.isDebugEnabled()){
            log.debug("API-SHIPMENT-EXPORT-END param: criteria [{}] ]",criteriaStr);
        }
    }

    /**
     * pos单导出
     * @param criteria
     */
    @GetMapping( value = "settlement/pos/export")
    public void exportSettlementPos(PoushengSettlementPosCriteria criteria){
        String criteriaStr = JsonMapper.nonEmptyMapper().toJson(criteria);
        if(log.isDebugEnabled()){
            log.debug("API-SETTLEMENT-POS-EXPORT-START param: criteria [{}] ]",criteriaStr);
        }
        List<Long> shopIds =  permissionUtil.getCurrentUserCanOperateShopIDs();
        if (criteria.getShopId()!=null&&!shopIds.contains(criteria.getShopId())){
            throw new JsonResponseException("permission.check.shop.id.empty");
        }
        if (criteria.getShopId()==null){
            criteria.setShopIds(shopIds);
        }
        ExportTradeBillEvent event = new ExportTradeBillEvent();
        event.setType(TradeConstants.EXPORT_POS);
        event.setCriteria(criteria);
        event.setUserId(UserUtil.getUserId());
        //换成Biz任务形式 modified by longjun.tlj
        String context=MAPPER.toJson(event);
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.EXPORT_TRADE_BILL.name());
        biz.setContext(context);
        biz.setBizId(TradeConstants.EXPORT_POS);
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.name());
        compensateBizLogic.createBizAndSendMq(biz,MqConstants.POSHENG_MIDDLE_EXPORT_COMPENSATE_BIZ_TOPIC);
        if(log.isDebugEnabled()){
            log.debug("API-SETTLEMENT-POS-EXPORT-END param: criteria [{}] ]",criteriaStr);
        }
    }

    /**
     * 导出文件记录
     * @return
     */
    @GetMapping("export/files")
    public Response<Paging<FileRecord>> exportFiles() {
        if(log.isDebugEnabled()){
            log.debug("API-EXPORT-FILES-START noparam: ");
        }
        List<FileRecord> files = exportService.getExportFiles();
        if(log.isDebugEnabled()){
            log.debug("API-EXPORT-FILES-END noparam: ,resp: [{}]",JsonMapper.nonEmptyMapper().toJson(files));
        }
        return Response.ok(new Paging<FileRecord>((long) files.size(), files));
    }


}
