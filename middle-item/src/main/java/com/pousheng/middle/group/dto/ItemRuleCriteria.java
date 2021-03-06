package com.pousheng.middle.group.dto;

import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;

import java.util.List;

/**
 * @author zhaoxw
 * @date 2018/5/8
 */

@Data
public class ItemRuleCriteria extends PagingCriteria {

    private Long id;

    private Integer type;
    
    private List<Long> ids;
}
