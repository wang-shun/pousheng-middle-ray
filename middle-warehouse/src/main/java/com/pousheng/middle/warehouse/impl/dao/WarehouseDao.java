package com.pousheng.middle.warehouse.impl.dao;

import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.model.Warehouse;
import io.terminus.common.model.Paging;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Author: jlchen
 * Desc: 仓库Dao类
 * Date: 2017-06-07
 */
@Repository
public class WarehouseDao extends MyBatisDao<Warehouse> {

    /**
     * 根据code做精确搜索
     *
     * @param code 仓库编码
     * @return 对应的仓库
     */
    public Warehouse findByCode(String code) {
        return getSqlSession().selectOne(sqlId("findByCode"), code);
    }

    /**
     * 根据code做模糊查询
     *
     * @param code 仓库编码
     * @return 符合条件的仓库列表
     */
    public List<Warehouse> findByFuzzyCode(String code) {
        return getSqlSession().selectList(sqlId("findByFuzzyCode"), code);
    }

    /**
     * 传入参数为name 可以为仓库名称或者仓库外码
     *
     * @param offset
     * @param limit
     * @param name
     * @return
     */
    public Paging<Warehouse> pagingByOutCodeOrName(Integer offset, Integer limit, String name) {
        Map<String, Object> criteria = Maps.newHashMap();
        criteria.put("name", name);
        Long total = (Long) this.sqlSession.selectOne(this.sqlId("countByOutCodeOrName"), criteria);
        if (total.longValue() <= 0L) {
            return new Paging(0L, Collections.emptyList());
        } else {
            ((Map) criteria).put("offset", offset);
            ((Map) criteria).put("limit", limit);
            List<Warehouse> datas = this.sqlSession.selectList(this.sqlId("pagingByOutCodeOrName"), criteria);
            return new Paging(total, datas);
        }
    }

}
