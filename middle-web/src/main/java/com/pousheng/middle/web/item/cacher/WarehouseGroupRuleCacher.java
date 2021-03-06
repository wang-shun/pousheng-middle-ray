package com.pousheng.middle.web.item.cacher;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.pousheng.middle.group.model.ItemRuleGroup;
import com.pousheng.middle.group.service.ItemRuleGroupReadService;
import com.pousheng.middle.group.service.ItemRuleShopReadService;
import com.pousheng.middle.group.service.ItemRuleWarehouseReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author zhaoxw
 * @date 2018/5/14
 */
@Component
@Slf4j
public class WarehouseGroupRuleCacher {

    private LoadingCache<Long, List<Long>> ruleCacher;

    @Value("${cache.duration.in.minutes: 60}")
    private Integer duration;

    @RpcConsumer
    private ItemRuleGroupReadService itemRuleGroupReadService;

    @RpcConsumer
    private ItemRuleWarehouseReadService itemRuleWarehouseReadService;

    @PostConstruct
    public void init() {
        this.ruleCacher = CacheBuilder.newBuilder()
                .expireAfterWrite(duration, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build(new CacheLoader<Long, List<Long>>() {
                    @Override
                    public List<Long> load(Long warehouseId) {
                        Response<Long> ruleResp = itemRuleWarehouseReadService.findRuleIdByWarehouseId(warehouseId);
                        if (!ruleResp.isSuccess()) {
                            log.error("failed to find rule (warehouseId={}, error:{}",
                                    warehouseId, ruleResp.getError());
                            throw new ServiceException("item.rule.find.fail");
                        }
                        if (ruleResp.getResult() == null) {
                            return Lists.newArrayList();
                        }
                        Response<List<ItemRuleGroup>> groupResp = itemRuleGroupReadService.findByRuleId(ruleResp.getResult());
                        if (!groupResp.isSuccess()) {
                            log.error("failed to find rule group (ruleId={}, error:{}",
                                    ruleResp.getResult(), ruleResp.getError());
                            throw new ServiceException("item.group.find.fail");
                        }
                        List<ItemRuleGroup> groups = groupResp.getResult();
                        return groups.stream().map(ItemRuleGroup::getGroupId).collect(Collectors.toList());
                    }
                });
    }

    public List<Long> findByWarehouseId(Long warehouseId) {
        return ruleCacher.getUnchecked(warehouseId);
    }

    /**
     * 刷新全部缓存，用于删除rule之后的缓存清理
     */
    public void refreshAll() {
        ruleCacher.invalidateAll();
    }
}

