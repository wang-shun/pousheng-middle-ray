package com.pousheng.middle.web.utils.operationlog;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.auth.model.OperatorExt;
import com.pousheng.auth.service.OperatorExtReadService;
import com.pousheng.middle.order.model.OperationLog;
import com.pousheng.middle.order.service.OperationLogWriteService;

import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.utils.UserUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.map.HashedMap;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Optional;

/**
 * Created by sunbo@terminus.io on 2017/7/31.
 */
@Slf4j
@Component
@Aspect
public class ProxyOperationLog {


    @Autowired
    private HttpServletRequest request;

    @Autowired
    private OperationLogWriteService operationLogWriteService;

    @Autowired
    private OperatorExtReadService operatorExtReadService;

    @Autowired
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Pointcut("execution(* com.pousheng.middle.web.order.*.*(..))")
    public void orderPointcut() {
    }

    @Pointcut("execution(* com.pousheng.middle.web.warehouses.*.*(..))")
    public void warehousePointcut() {
    }

    @Pointcut("execution(* com.pousheng.middle.web.item.*.*(..))")
    public void itemPointcut() {
    }




    @AfterReturning("orderPointcut() || warehousePointcut() || itemPointcut()")
    public void record(JoinPoint pjp) {


        MethodSignature signature = (MethodSignature) pjp.getSignature();

        if (signature.getMethod().isAnnotationPresent(OperationLogIgnore.class)) {
            log.debug("[{}.{}] annotation OperationLogIgnore,ignore record operation log", pjp.getTarget().getClass().getName(), signature.getMethod().getName());
            return;
        }

        if (!signature.getMethod().isAnnotationPresent(OperationLogType.class)
                && request.getMethod().equalsIgnoreCase("GET")) {
            log.debug("[{}.{}] not annotation OperationLogType and request by http GET,ignore record operation log", pjp.getTarget().getClass().getName(), signature.getMethod().getName());
            return;
        }

        if (!signature.getMethod().isAnnotationPresent(RequestMapping.class)
                && !signature.getMethod().isAnnotationPresent(PostMapping.class)
                && !signature.getMethod().isAnnotationPresent(PutMapping.class)
                && !signature.getMethod().isAnnotationPresent(DeleteMapping.class)
                && !signature.getMethod().isAnnotationPresent(GetMapping.class)) {
            log.debug("[{}.{}] not annotation RequestMapping or PostMapping or PutMapping or DeleteMapping or GetMapping,ignore record operation log", pjp.getTarget().getClass().getName(), signature.getMethod().getName());
            return;
        }

        if (null == UserUtil.getCurrentUser()) {
            log.info("no user login,record operation log abort");
            return;
        }

        String name = UserUtil.getCurrentUser().getName();
        String realName = null;
        OperationLogModule moduleAnno = signature.getMethod().getDeclaredAnnotation(OperationLogModule.class);
        if (null == moduleAnno)
            moduleAnno = pjp.getTarget().getClass().getDeclaredAnnotation(OperationLogModule.class);

        if (!StringUtils.isEmpty(name)) {
        	Response<OperatorExt> operatorResp = operatorExtReadService.findByUserName(name);
        	if (!operatorResp.isSuccess()) {
        		log.warn("find operator failed by username:{}, cause:{}", 
        				name, operatorResp.getError());
        		throw new JsonResponseException(operatorResp.getError());
        	}
        	realName = operatorResp.getResult().getRealName();
        }
        
        OperationLog log = new OperationLog();
        log.setOperatorName(name);
        log.setRealName(realName);
        log.setContent(getContent(signature.getMethod().getDeclaredAnnotation(OperationLogType.class), signature.getParameterNames(), pjp.getArgs()));
        log.setType(getType(moduleAnno, request.getRequestURI()));
        log.setOperateId(getKey(signature, pjp).orElse(""));

        eventBus.post(new OperationLogEvent(log));
    }


    @Subscribe
    public void record(OperationLogEvent event) {
        Response resp = operationLogWriteService.create(event.getLog());
        if (!resp.isSuccess())
            log.error("record operation log fail,{}", resp.getError());

    }


    private Integer getType(OperationLogModule operationLogModule, String url) {

        if (null != operationLogModule)
            return operationLogModule.value().getValue();

        if (url.length() <= 5)
            return OperationLogModule.Module.UNKNOWN.getValue();

        //remove '/api/'
        String key = url.substring(5);
        key = key.substring(0, key.indexOf('/'));
        return OperationLogModule.Module.fromKey(key).getValue();
    }

    private String getContent(OperationLogType operationLogTypeAnno, String[] parameterNames, Object[] args) {

        String operationType = null;
        if (null != operationLogTypeAnno) {
            operationType = operationLogTypeAnno.value();
        } else {
            switch (request.getMethod().toUpperCase()) {
                case "PUT":
                    operationType = "新增";
                    break;
                case "POST":
                    operationType = "修改";
                    break;
                case "DELETE":
                    operationType = "删除";
                    break;
                default:
                    operationType = "未知";
            }
        }

        Map<Object, Object> content = new HashedMap();
        content.put("type", operationType);
        for (int i = 0; i < parameterNames.length; i++) {
            if(args[i] instanceof MultipartFile){
                args[i] = ((MultipartFile)args[i]).getOriginalFilename();
            }
            content.put(parameterNames[i], args[i]);
        }

        return JsonMapper.nonDefaultMapper().toJson(content);
    }


    private Optional<String> getKey(MethodSignature signature, JoinPoint pjp) {

        Object[] args = pjp.getArgs();
        int pos = 0;
        for (Parameter parameter : signature.getMethod().getParameters()) {
            if (parameter.isAnnotationPresent(OperationLogParam.class)) {
                if (null == args[pos])
                    return Optional.ofNullable(null);
                else return Optional.of(args[pos].toString());
            }
            pos++;
        }

        log.info("[{}.{}]can not find key parameter with OperationLogParam annotation,start automatic match", pjp.getTarget().getClass().getName(), signature.getMethod().getName());
        if (args.length == 1 && signature.getParameterNames()[0].toUpperCase().contains("ID")) {
            return Optional.ofNullable(null == args[0] ? null : args[0].toString());
        }

        pos = 0;
        for (String name : signature.getParameterNames()) {
            if (name.equalsIgnoreCase("id")) {
                return Optional.ofNullable(null == args[pos] ? null : args[pos].toString());
            }
            pos++;
        }
        return Optional.ofNullable(null);
    }


    @Data
    @AllArgsConstructor
    class OperationLogEvent {
        private OperationLog log;
    }
}
