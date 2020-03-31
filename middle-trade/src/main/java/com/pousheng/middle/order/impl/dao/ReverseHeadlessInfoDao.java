package com.pousheng.middle.order.impl.dao;

import com.pousheng.middle.order.model.ReverseHeadlessInfo;
import com.pousheng.middle.order.model.ReverseInstoreInfo;
import io.terminus.common.model.Response;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Code generated by terminus code gen
 * Desc: Dao类
 * Date: 2019-06-03
 */
@Repository
public class ReverseHeadlessInfoDao extends MyBatisDao<ReverseHeadlessInfo> {

    public ReverseHeadlessInfo findReverseHeadlessInfoByUniqueNo(String uniqueNo) {
        return this.sqlSession.selectOne(sqlId("findReverseHeadlessInfoByUniqueNo"),uniqueNo);
    }

    public Long countHeadLeass(Map<String, Object> reverseHeadlessCriteria){
       return (Long)this.sqlSession.selectOne(this.sqlId("count"), reverseHeadlessCriteria);
    }

    public int batchCreate(List<ReverseHeadlessInfo> list) {
        return this.sqlSession.insert(sqlId("batchCreate"), list);
    }

}
