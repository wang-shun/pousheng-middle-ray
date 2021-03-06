package com.pousheng.middle.auth;

import com.google.common.base.Throwables;
import com.pousheng.auth.model.MiddleUser;
import com.pousheng.auth.service.PsUserReadService;
import io.terminus.common.model.Response;
import io.terminus.parana.auth.api.*;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 中台用户角色加载器
 *
 * @author songrenfei
 */
@Slf4j
@Component
public class MiddleUserRoleLoader implements UserRoleLoader {

    @Autowired
    private PsUserReadService userReadService;

    @Autowired
    private RoleProviderRegistry roleProviderRegistry;

    @Override
    public Response<RoleContent> hardLoadRoles(Long userId) {
        try {
            if (userId == null) {
                log.warn("hard load roles failed, userId=null");
                return Response.fail("middleUser.id.empty");
            }
            val findResp = userReadService.findById(userId);
            if (!findResp.isSuccess()) {
                log.warn("find middleUser failed, userId={}, error={}", userId, findResp.getError());
                return Response.fail(findResp.getError());
            }
            MiddleUser middleUser = findResp.getResult();
            if (middleUser == null) {
                // findById 已经保证不会进入这里
                log.warn("hard load roles failed, middleUser not found, id={}", userId);
                return Response.fail("middleUser.not.found");
            }

            if (middleUser.getType() == null) {
                log.warn("middleUser has no type, userId={}, we treat is as empty permission", userId);
                return Response.ok(initRoles());
            }
            int userType = middleUser.getType();

            RoleContent mutableRoles = initRoles();

            List<RoleProvider> roleProviders = roleProviderRegistry.getRoleProviders();
            if (!CollectionUtils.isEmpty(roleProviders)) {
                for (RoleProvider roleProvider : roleProviders) {
                    if (roleProvider.acceptType() != userType) {
                        continue;
                    }
                    Role role = roleProvider.getRoleByUserId(userId);
                    if (role != null) {
                        if (role.getType() == 1) {
                            // static
                            mutableRoles.getRoles().add(role);
                        } else {
                            mutableRoles.getDynamicRoles().add(role);
                        }
                    }
                }
            }
            return Response.ok(mutableRoles);
        } catch (Exception e) {
            log.error("hard load rich roles failed, userId={}, cause:{}",
                    userId, Throwables.getStackTraceAsString(e));
            return Response.fail("user.role.load.fail");
        }
    }

    private RoleContent initRoles() {
        RoleContent non = new RoleContent();
        non.setRoles(new ArrayList<Role>());
        non.setDynamicRoles(new ArrayList<Role>());
        return non;
    }

}
