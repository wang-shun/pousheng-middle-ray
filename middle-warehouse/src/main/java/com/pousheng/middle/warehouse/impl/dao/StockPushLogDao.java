package com.pousheng.middle.warehouse.impl.dao;

import com.pousheng.middle.warehouse.model.StockPushLog;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/10
 * pousheng-middle
 */
@Repository
public class StockPushLogDao extends MyBatisDao<StockPushLog> {

    public int batchUpdateResultByRequestIdAndLineNo(List<StockPushLog> stockPushLogs) {
        return getSqlSession().update(sqlId("batchUpdateResultByRequestIdAndLineNo"), stockPushLogs);
    }
}
