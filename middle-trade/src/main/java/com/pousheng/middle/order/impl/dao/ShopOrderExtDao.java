package com.pousheng.middle.order.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.pousheng.middle.order.model.ShopOrderExt;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

/**
 * Created by tony on 2017/8/10.
 * pousheng-middle
 */
@Repository
public class ShopOrderExtDao extends MyBatisDao<ShopOrderExt> {

    public boolean updateHandleStatus(Long id, String newHandleStatus, String originHandleStatus) {
        return getSqlSession().update(sqlId("updateHandleStatus"), ImmutableMap.of("id", id, "newHandleStatus", newHandleStatus, "originHandleStatus", originHandleStatus)) == 1;
    }
}
