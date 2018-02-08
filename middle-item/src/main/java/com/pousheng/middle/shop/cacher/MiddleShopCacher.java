/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.shop.cacher;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.pousheng.middle.shop.service.PsShopReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Joiners;
import io.terminus.common.utils.Splitters;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-04-26
 */
@Component
@Slf4j
public class MiddleShopCacher {

    private LoadingCache<String, Shop> shopCacher;

    @RpcConsumer
    private PsShopReadService shopReadService;

    @Value("${cache.duration.in.minutes: 60}")
    private Integer duration;

    @PostConstruct
    public void init() {
        this.shopCacher = CacheBuilder.newBuilder()
                .expireAfterWrite(duration, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build(new CacheLoader<String, Shop>() {
                    @Override
                    public Shop load(String joinStr) throws Exception {
                        List<String> stringList = Splitters.COLON.splitToList(joinStr);
                        String outerId = stringList.get(0);
                        Long businessId = Long.valueOf(stringList.get(1));
                        Response<Optional<Shop>> rShop = shopReadService.findByOuterIdAndBusinessId(outerId,businessId);
                        if (!rShop.isSuccess()) {
                            log.error("failed to find shop(outerId={},businessId={}), error code:{}",
                                    outerId,businessId,rShop.getError());
                            throw new ServiceException("find.shop.fail");
                        }
                        if(!rShop.getResult().isPresent()){
                            log.error("not find shop(outerId={},businessId={})",
                                    outerId,businessId);
                            throw new ServiceException("shop.not.exist");

                        }
                        return rShop.getResult().get();
                    }
                });
    }

    /**
     * 根据outerId查找shop的信息
     *
     * @param outerId shop outer id
     * @param businessId shop business id
     * @return 对应shop信息
     */
    public Shop findByOuterIdAndBusinessId(String outerId,Long businessId) {
        return shopCacher.getUnchecked(Joiners.COLON.join(outerId,businessId));
    }

    /**
     * 根据outerId刷新shop的信息
     *
     * @param outerId shop outer id
     * @param businessId shop business id
     * @return 对应shop信息
     */
    public void refreshByOuterIdAndBusinessId(String outerId,Long businessId) {
         shopCacher.refresh(Joiners.COLON.join(outerId,businessId));
    }
}