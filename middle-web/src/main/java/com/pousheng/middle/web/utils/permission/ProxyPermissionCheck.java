package com.pousheng.middle.web.utils.permission;

import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;

/**
 * Created by sunbo@terminus.io on 2017/7/28.
 */
@Slf4j
@Aspect
@Component
@Order(1)//切面最先执行
public class ProxyPermissionCheck {


    @Autowired
    private PermissionUtil permissionUtil;


    @Before("execution(* com.pousheng.middle.web.order.*.*(..))")
    public void check(JoinPoint pjp) throws Throwable {

        MethodSignature signature = (MethodSignature) pjp.getSignature();

        int permissionCheckParamPos = 0;
        boolean hasPermissionCheckParamMarked = false;
        Field fieldNeedToCheck = null;
        for (Parameter parameter : signature.getMethod().getParameters()) {
            if (parameter.isAnnotationPresent(PermissionCheckParam.class)) {

                if (parameter.getType().getName().equals(Long.class.getName()) ||
                        parameter.getType().getName().equals(long.class.getName())) {
                    hasPermissionCheckParamMarked = true;
                    break;
                } else {
                    PermissionCheckParam checkParam = parameter.getAnnotation(PermissionCheckParam.class);
                    if (StringUtils.isNotBlank(checkParam.value())) {
                        String name = checkParam.value().trim();
                        try {
                            fieldNeedToCheck = parameter.getType().getDeclaredField(name);
                            hasPermissionCheckParamMarked = true;
                            break;
                        } catch (NoSuchFieldException e) {
                            log.warn("can not find field [" + name + "] in class [" + parameter.getType().getName() + "],permission check abort");
                        }
                    }
                }
            }
            permissionCheckParamPos++;
        }


        if (hasPermissionCheckParamMarked) {

            log.debug("this method [{}] has marked permission check", signature.getMethod().getName());

            if (pjp.getArgs()[0] == null) {
                log.info("permission check for method [{}] is aboard,parameter is null", signature.getMethod().getName());
            } else {

                PermissionCheck permissionCheckAnno = signature.getMethod().getAnnotation(PermissionCheck.class);
                if (null == permissionCheckAnno)
                    permissionCheckAnno = pjp.getTarget().getClass().getAnnotation(PermissionCheck.class);
                if (null == permissionCheckAnno) {
                    log.info("can not find PermissionCheck annotation on method or class,permission check for [{}.{}] abort", pjp.getTarget().getClass().getName(), signature.getMethod().getName());
                    return;
                }

                Long id;
                if (null != fieldNeedToCheck) {
                    try {
//                        Field field = pjp.getArgs()[0].getClass().getField(fieldName);
                        fieldNeedToCheck.setAccessible(true);
                        id = (Long) fieldNeedToCheck.get(pjp.getArgs()[0]);
                    } catch (Exception e) {
                        throw new JsonResponseException("permission.check.access.field.fail");
//                        return permissionDeny("permission.check.access.field.fail", signature.getReturnType().getName(), permissionCheckAnno.throwExceptionWhenPermissionDeny());
                    }
                } else
                    id = (Long) pjp.getArgs()[permissionCheckParamPos];


                Response<Boolean> permissionCheckResponse;
                if (permissionCheckAnno.value() == PermissionCheck.PermissionCheckType.SHOP_ORDER) {
                    permissionCheckResponse = permissionUtil.checkByShopOrderID(id);
                } else if (permissionCheckAnno.value() == PermissionCheck.PermissionCheckType.SHOP)
                    permissionCheckResponse = permissionUtil.checkByShopID(id);
                else if (permissionCheckAnno.value() == PermissionCheck.PermissionCheckType.REFUND)
                    permissionCheckResponse = permissionUtil.checkByRefundID(id);
                else
                    permissionCheckResponse = permissionUtil.checkByShipmentID(id);

                if (!permissionCheckResponse.isSuccess()) {
                    throw new JsonResponseException(permissionCheckResponse.getError());
//                    return permissionDeny(permissionCheckResponse.getError(), signature.getReturnType().getName(), permissionCheckAnno.throwExceptionWhenPermissionDeny());
                }
            }
        }

//        return pjp.proceed();
    }


    private Response permissionDeny(String error, String returnTypeName, boolean throwExceptionWhenPermissionDeny) {
        if (throwExceptionWhenPermissionDeny)
            throw new JsonResponseException(error);


//        String returnTypeName = signature.getReturnType().getName();
        if (returnTypeName.equals(Response.class.getName())) {
            return Response.fail(error);
        } else throw new JsonResponseException(error);
    }

}
