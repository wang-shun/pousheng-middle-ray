package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.WarehouseShopRule;
import io.terminus.common.model.Response;

import java.util.List;
import java.util.Set;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-05
 */
public interface WarehouseShopRuleReadService {

    /**
     * 根据规则找对应的店铺列表
     * @param ruleId 规则id
     * @return 对应的店铺列表
     */
    Response<List<WarehouseShopRule>> findByRuleId(Long ruleId);

    /**
     * 获取已设置发货规则的店铺id集合
     * @return 已设置发货规则的店铺id集合
     */
    Response<Set<Long>> findShopIds();
}
