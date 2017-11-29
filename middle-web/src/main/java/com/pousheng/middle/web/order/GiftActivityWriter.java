package com.pousheng.middle.web.order;

import com.pousheng.middle.order.dto.ActivityItem;
import com.pousheng.middle.order.dto.EditSubmitGiftActivityInfo;
import com.pousheng.middle.order.dto.GiftItem;
import com.pousheng.middle.order.dto.SubmitRefundInfo;
import com.pousheng.middle.order.dto.fsm.PoushengGiftActivityEvent;
import com.pousheng.middle.order.model.PoushengGiftActivity;
import com.pousheng.middle.order.service.PoushengGiftActivityReadService;
import com.pousheng.middle.order.service.PoushengGiftActivityWriteService;
import com.pousheng.middle.web.order.component.PoushengGiftActivityWriteLogic;
import com.pousheng.middle.web.utils.operationlog.OperationLogModule;
import com.pousheng.middle.web.utils.operationlog.OperationLogType;
import com.pousheng.middle.web.utils.permission.PermissionCheck;
import com.pousheng.middle.web.utils.permission.PermissionCheckParam;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Objects;


/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/29
 * pousheng-middle
 */
@RestController
@Slf4j
@OperationLogModule(OperationLogModule.Module.POUSHENG_GIFT_ACTIVITY)
public class GiftActivityWriter {

    @Autowired
    private PoushengGiftActivityWriteLogic poushengGiftActivityWriteLogic;
    @RpcConsumer
    private PoushengGiftActivityWriteService poushengGiftActivityWriteService;
    @RpcConsumer
    private PoushengGiftActivityReadService poushengGiftActivityReadService;
    /**
     * 创建活动规则
     *
     * @param editSubmitGiftActivityInfo 提交信息
     * @return
     */
    @RequestMapping(value = "/api/gift/actvity/create", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("创建活动规则")
    public Response<Long> createGiftActivity(@RequestBody EditSubmitGiftActivityInfo editSubmitGiftActivityInfo) {
        return Response.ok(poushengGiftActivityWriteLogic.createGiftActivity(editSubmitGiftActivityInfo));
    }

    /**
     * 创建活动规则
     * @return
     */
    @RequestMapping(value = "/api/gift/actvity/create", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("创建活动规则")
    public Response<Long> createTest(@RequestParam(required =false)String name,@RequestParam(required = false) String startAt,@RequestParam(required = false) String endAt,@RequestParam Integer activityType,
                                     @RequestParam(required = false) Integer fee,@RequestParam(required = false)Integer quantity,Boolean isNoLimitItem,Integer limitQuantity
                                    ,@RequestParam(required = false)Long shopId,@RequestParam(required = false) String shopName,@RequestParam(required = false) Integer price
                                    ,@RequestParam(required = false)String giftSkuCode,@RequestParam(required = false)String activitySkuCode) throws ParseException {
        EditSubmitGiftActivityInfo editSubmitGiftActivityInfo = new EditSubmitGiftActivityInfo();
        editSubmitGiftActivityInfo.setName(name);
        Date startDate = DateUtils.parseDate(startAt,"yyyyMMdd HH:mm:ss");
        Date endDate = DateUtils.parseDate(endAt,"yyyyMMdd HH:mm:ss");
        editSubmitGiftActivityInfo.setActivityStartDate(startDate);
        editSubmitGiftActivityInfo.setActivityEndDate(endDate);
        editSubmitGiftActivityInfo.setActivityType(activityType);
        editSubmitGiftActivityInfo.setQuantity(quantity);
        editSubmitGiftActivityInfo.setFee(fee);
        editSubmitGiftActivityInfo.setIsNoLimitItem(isNoLimitItem);
        editSubmitGiftActivityInfo.setLimitQuantity(limitQuantity);
        List<ActivityItem> activityItems = Lists.newArrayList();
        ActivityItem activityItem = new ActivityItem();
        activityItem.setSkuCode(activitySkuCode);
        activityItems.add(activityItem);
        List<GiftItem> giftItems = Lists.newArrayList();
        GiftItem giftItem = new GiftItem();
        giftItem.setSkuCode(giftSkuCode);

        return Response.ok(poushengGiftActivityWriteLogic.createGiftActivity(editSubmitGiftActivityInfo));
    }


    /**
     * 更新活动规则
     *
     * @param editSubmitGiftActivityInfo 提交信息
     * @return
     */
    @RequestMapping(value = "/api/gift/actvity/update", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("更新活动规则")
    public Response<Boolean> updateGiftActivity(@RequestBody EditSubmitGiftActivityInfo editSubmitGiftActivityInfo) {
        return Response.ok(poushengGiftActivityWriteLogic.updateGiftActivity(editSubmitGiftActivityInfo));
    }

    /**
     *发布活动
     * @param id
     * @return
     */
    @RequestMapping(value = "/api/gift/actvity/{id}/publish", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> publishGiftActivity(@PathVariable("id") Long id){
        return poushengGiftActivityWriteLogic.updatePoushengGiftActivityStatus(id, PoushengGiftActivityEvent.PUBLISH.toOrderOperation());
    }

    /**
     * 使活动失效
     */
    @RequestMapping(value = "/api/gift/actvity/{id}/over", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> overGiftActivity(@PathVariable("id") Long id){
        return poushengGiftActivityWriteLogic.updatePoushengGiftActivityStatus(id, PoushengGiftActivityEvent.OVER.toOrderOperation());
    }

    /**
     * 删除活动
     */
    @RequestMapping(value = "/api/gift/actvity/{id}/delete", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> deleteGiftActivity(@PathVariable("id") Long id){
        return poushengGiftActivityWriteLogic.updatePoushengGiftActivityStatus(id, PoushengGiftActivityEvent.DELETE.toOrderOperation());
    }
}
