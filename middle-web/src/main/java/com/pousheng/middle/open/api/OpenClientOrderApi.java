package com.pousheng.middle.open.api;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.mq.constant.MqConstants;
import com.pousheng.middle.open.component.OpenOrderConverter;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.job.order.component.OrderExecutor;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.open.client.common.shop.service.OpenShopWriteService;
import io.terminus.open.client.order.dto.*;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.ShopOrderReadService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 接收外部推送的订单
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/16
 * open-client
 */

@OpenBean
@Slf4j
public class OpenClientOrderApi {
    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;
    @RpcConsumer
    private OpenShopReadService openShopReadService;
    @RpcConsumer
    private OpenShopWriteService openShopWriteService;
    @Autowired
    private OpenShopCacher openShopCacher;
    @Autowired
    private OrderExecutor orderExecutor;
    @Autowired
    private OpenOrderConverter openOrderConverter;

    @Autowired
    private CompensateBizLogic compensateBizLogic;
    @Autowired
    private JedisTemplate jedisTemplate;
    @Value("${current.ip}")
    private String currentIpAddress;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();


    //根据渠道判断订单是否插入中台
    private static final String IS_ORDER_INSERT_MIDDLE = "isOrderInsertMiddle";

    @OpenMethod(key = "push.out.open.order.api", paramNames = {"orderInfo"}, httpMethods = RequestMethod.POST)
    public void receiveOrders(@NotEmpty String orderInfo) {
        log.info("receive open orders start...");
        log.info("received open client order info param is {}", orderInfo);
        List<OpenFullOrderInfo> orders = JsonMapper.nonEmptyMapper()
                .fromJson(orderInfo, JsonMapper.nonEmptyMapper().createCollectionType(List.class, OpenFullOrderInfo.class));
        if (CollectionUtils.isEmpty(orders)) {
            log.error("request parameter string={} are illegal", orderInfo);
            throw new OPServerException(200, "parameters are illegal ");
        }
        for (OpenFullOrderInfo openFullOrderInfo : orders) {
            try {

                if (isExist(openFullOrderInfo.getOrder().getOutId())) {//修复因并发或超时，出现重复创单
                    log.info("push.out.open.order.api order exist, skip, orderInfo({})", openFullOrderInfo.getOrder());
                    continue;
                }
                saveFlag(openFullOrderInfo.getOrder().getOutId(), currentIpAddress);
                //参数校验
                this.validateParam(openFullOrderInfo);
                //查询该渠道的店铺信息
                String shopCode = openFullOrderInfo.getOrder().getCompanyCode() + "-" + openFullOrderInfo.getOrder().getShopCode();
                Long openShopId = this.validateOpenShop(shopCode);
                OpenShop openShop = openShopCacher.findById(openShopId);
                Map<String, String> openShopExtra = openShop.getExtra();
                String isOrderInsertMiddle = openShopExtra.get(IS_ORDER_INSERT_MIDDLE);
                PoushengCompensateBiz biz = new PoushengCompensateBiz();
                biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
                biz.setBizId(openShopId.toString());
                //判断该订单是否需要存放到中台
                if (StringUtils.isEmpty(isOrderInsertMiddle) || Objects.equals(isOrderInsertMiddle, "true")) {
                    //业务参数校验
                    this.validateBusiParam(openFullOrderInfo);
                    //组装参数
                    OpenClientFullOrder openClientFullOrder = openOrderConverter.transform(openFullOrderInfo, openShop);
                    biz.setBizType(PoushengCompensateBizType.OPEN_ORDER_IMPORT.toString());
                    biz.setContext(mapper.toJson(openClientFullOrder));
                } else {
                    //订单不插入中台
                    biz.setBizType(PoushengCompensateBizType.OPEN_ORDER_PUSH.toString());
                    biz.setContext(mapper.toJson(openFullOrderInfo));
                }
                if (!canHandle(openFullOrderInfo.getOrder().getOutId(), currentIpAddress)) {
                    log.info("other ip exec , skip. orderInfo:({})", orderInfo);
                    continue;
                }
                compensateBizLogic.createBizAndSendMq(biz, MqConstants.POSHENG_MIDDLE_COMMON_COMPENSATE_BIZ_TOPIC);
            } catch (ServiceException se) {
                removeFlag(openFullOrderInfo.getOrder().getOutId());
                if (!"shop.order.is.exist".equals(se.getMessage())) {
                    throw new OPServerException(200, se.getMessage());
                }
            } catch (Exception e) {
                removeFlag(openFullOrderInfo.getOrder().getOutId());
                log.error("create open  order:{} failed,caused by {}", orderInfo, Throwables.getStackTraceAsString(e));
                throw new OPServerException(200, "create.middle.order.fail");
            }
        }

        log.info("receive open orders end");
    }


    /**
     * 参数校验
     *
     * @param openFullOrderInfo
     */
    private void validateParam(OpenFullOrderInfo openFullOrderInfo) {
        if (Objects.isNull(openFullOrderInfo)) {
            throw new ServiceException("openFullOrderInfo.is.null");
        }
        OpenFullOrder openFullOrder = openFullOrderInfo.getOrder();
        if (Objects.isNull(openFullOrder)) {
            throw new ServiceException("openFullOrder.is.null");
        }
        List<OpenFullOrderItem> items = openFullOrderInfo.getItem();
        if (Objects.isNull(items) || items.isEmpty()) {
            throw new ServiceException("openFullOrderItems.is.null");
        }
        OpenFullOrderAddress address = openFullOrderInfo.getAddress();
        if (Objects.isNull(address)) {
            throw new ServiceException("openFullOrderAddress.is.null");
        }
    }


    /**
     * 业务参数校验
     *
     * @param openFullOrderInfo
     */
    private void validateBusiParam(OpenFullOrderInfo openFullOrderInfo) {
        OpenFullOrder openFullOrder = openFullOrderInfo.getOrder();
        if (Objects.isNull(openFullOrder.getOutOrderId())) {
            throw new ServiceException("outOrderId.is.null");
        }
        if (Objects.isNull(openFullOrder.getChannel())) {
            throw new ServiceException("channel.is.null");
        }
        String outId = openFullOrder.getOutOrderId();
        String channel = openFullOrder.getChannel();
        Response<Optional<ShopOrder>> rP = shopOrderReadService.findByOutIdAndOutFrom(outId, channel);
        if (!rP.isSuccess()) {
            log.error("find shopOrder failed,outId is {},outFrom is {},caused by {}", outId, channel, rP.getError());
        }
        Optional<ShopOrder> shopOrderOptional = rP.getResult();
        if (shopOrderOptional.isPresent()) {
            throw new ServiceException("shop.order.is.exist");
        }
    }

    /**
     * 查询外部渠道
     *
     * @param shopCode
     * @return
     */
    public Long validateOpenShop(String shopCode) {

        //查询店铺的信息，如果没有就新建一个
        Response<List<OpenClientShop>> rP = openShopReadService.search(null, null, shopCode);
        if (!rP.isSuccess()) {
            log.error("find open shop failed,shopCode is {},caused by {}", shopCode, rP.getError());
            throw new ServiceException("find.open.shop.failed");
        }
        List<OpenClientShop> openClientShops = rP.getResult();
        if (CollectionUtils.isEmpty(openClientShops)) {
            throw new OPServerException(200, "find.open.shop.fail");
        }
        java.util.Optional<OpenClientShop> openClientShopOptional = openClientShops.stream().findAny();
        OpenClientShop openClientShop = openClientShopOptional.get();
        return openClientShop.getOpenShopId();

    }

    private OpenShop findById(Long openShopId) {
        Response<OpenShop> r = openShopReadService.findById(openShopId);
        if (!r.isSuccess()) {
            log.error("find open shop by id failed,openShopId is {},caused by {}", openShopId, r.getError());
            throw new ServiceException("find.open.shop.failed");
        }
        return r.getResult();
    }

    /**
     * 订单信息创建task任务
     *
     * @param openFullOrderInfo
     */
    private void createOpenOrderTask(OpenFullOrderInfo openFullOrderInfo) {
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.OUT_OPEN_ORDER.toString());
        biz.setContext(mapper.toJson(openFullOrderInfo));
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        compensateBizLogic.createBizAndSendMq(biz, MqConstants.POSHENG_MIDDLE_COMMON_COMPENSATE_BIZ_TOPIC);
    }

    private final String OPEN_RECEIVE_ORDER_PREFIX = "OPEN_RECEIVE_ORDER_PREFIX_";

    private Boolean isExist(String outId) {
        return jedisTemplate.execute(jedis -> {
            return jedis.exists(OPEN_RECEIVE_ORDER_PREFIX + outId);
        });
    }

    private Boolean canHandle(String outId, String flag) {
        String value = jedisTemplate.execute(jedis -> {
            return jedis.get(OPEN_RECEIVE_ORDER_PREFIX + outId);
        });
        log.info("final exec ip is value:({}) by outId:({})", value, outId);
        return flag.equals(value);
    }

    private void saveFlag(String outId, String flag) {
        jedisTemplate.execute(jedis -> {
            jedis.setex(OPEN_RECEIVE_ORDER_PREFIX + outId, 7200, flag);
        });
    }

    private void removeFlag(String outId) {
        jedisTemplate.execute(jedis -> {
            jedis.del(OPEN_RECEIVE_ORDER_PREFIX + outId);
        });
    }
}
