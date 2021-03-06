/*
 * Copyright (c) 2017. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : panxin
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PsShop {

    private String id;

    //内码
    private String storeId;

    private String name;

    private String code;

    private String companyId;

    private String companyName;

    private String zoneId;

    private String zoneName;

    //"店铺电话", position = 19)
    private String telphone;

    //"邮箱", position = 19)
    private String email;

    // "店铺地址", position = 18)
    private String address;


}
