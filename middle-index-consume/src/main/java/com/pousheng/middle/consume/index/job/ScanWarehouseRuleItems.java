package com.pousheng.middle.consume.index.job;

import com.pousheng.inventory.api.service.WarehouseRuleItemReadService;
import com.pousheng.middle.consume.index.processor.core.IndexEvent;
import com.pousheng.middle.consume.index.processor.impl.sendRule.StockWarehouseProcessor;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
import org.assertj.core.util.Strings;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AUTHOR: zhangbin
 * ON: 2019/7/31
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "warehouse.shop.group.scan", havingValue = "true", matchIfMissing = false)
public class ScanWarehouseRuleItems {

    //pousheng_warehouse_rule_items
    @RpcConsumer(version = "1.0.0")
    private WarehouseRuleItemReadService warehouseRuleItemReadService;

    @Value("${warehouse.shop.group.scan.seconds:60}")
    private Integer seconds;

    @Autowired
    private JedisTemplate jedisTemplate;
    @Autowired
    private HostLeader hostLeader;
    @Autowired
    private StockWarehouseProcessor stockWarehouseProcessor;

    private String key = "WAREHOUSE_RULE_ITEM_SCAN_TIME";

    @Scheduled(fixedDelay = 10000)
    public void scan() {
        log.info("warehouse rule items scan start ");
        if(!hostLeader.isLeader()) {
            log.info("warehouse rule items scan current leader is:{}, skip ", hostLeader.currentLeaderId());
            return;
        }
        DateTime start;
        String time = jedisTemplate.execute(jedis -> {
            return jedis.get(key);
        });
        log.info("redis time {} ", time);
        DateTime now = DateTime.now();
        if (Strings.isNullOrEmpty(time)) {
            start = now.minusSeconds(seconds);
        } else {
            start = DateTime.parse(time).minusSeconds(seconds);
        }
        jedisTemplate.execute(jedis -> {
            jedis.set(key, now.toString());
        });
        Response<List<Long>> idsResp = warehouseRuleItemReadService.findGreaterThanUpdatedAt(start.toDate());
        if (!idsResp.isSuccess()) {
            log.error("warehouse rule items scan fail , cause:({})", idsResp.getError());
            return;
        }
        log.info("find ids {} ", idsResp.getResult());

        HashSet<Long> idSet = Sets.newHashSet(idsResp.getResult());
        stockWarehouseProcessor.doProcess(idSet);
        log.info("warehouse rule items scan end ");
    }


}
