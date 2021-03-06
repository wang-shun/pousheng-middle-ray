package com.pousheng.middle.open.manager;

import io.terminus.pampas.openplatform.core.OPHook;
import io.terminus.pampas.openplatform.core.SecurityManager;
import io.terminus.pampas.openplatform.entity.OPClientInfo;
import org.springframework.stereotype.Component;

/**
 * Author:cp
 * Created on 8/12/16.
 */
@Component
public class MiddleOpenSecurityManager implements SecurityManager {

    @Override
    public OPClientInfo findClientByAppKey(String appKey) {
        //just for dev
        return new OPClientInfo(1L, appKey,"6a0e@93204aefe45d47f6e488");
    }

    @Override
    public OPClientInfo findClientById(Long clientId) {
        return new OPClientInfo(clientId, "pousheng","6a0e@93204aefe45d47f6e488");
    }

    @Override
    public boolean hasPermission(Long clientId, String method) {
        return true;
    }

    @Override
    public OPHook getHook(Long clientId, String method) {
        return null;
    }
}
