package com.pousheng.middle.web.user;

import com.google.common.base.CharMatcher;
import com.google.common.collect.Lists;
import com.pousheng.auth.dto.UcUserInfo;
import com.pousheng.auth.model.User;
import com.pousheng.auth.service.UserReadService;
import com.pousheng.auth.service.UserWriteService;
import com.pousheng.middle.web.user.component.UcUserOperationLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Params;
import io.terminus.parana.auth.model.Operator;
import io.terminus.parana.auth.service.OperatorReadService;
import io.terminus.parana.auth.service.OperatorWriteService;
import io.terminus.parana.common.enums.UserRole;
import io.terminus.parana.common.enums.UserType;
import io.terminus.parana.common.utils.EncryptUtil;
import io.terminus.parana.common.utils.RespHelper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * @author Effet
 */
@Slf4j
@RestController
@RequestMapping("/api/operator")
public class OperatorApis {

    @Autowired
    private UserWriteService userWriteService;
    @Autowired
    private UserReadService userReadService;
    @RpcConsumer
    private OperatorReadService operatorReadService;
    @RpcConsumer
    private OperatorWriteService operatorWriteService;
    @Autowired
    private UcUserOperationLogic ucUserOperationLogic;

    /**
     * ADMIN 创建运营
     *
     * @param operator 运营信息
     * @return 运营用户 ID
     */
    @RequestMapping(value = "", method = RequestMethod.POST)
    public Long createOperator(@RequestBody OperatorPost operator) {
        String un = Params.trimToNull(operator.getUsername());
        if (un == null) {
            log.warn("create operator failed, no username specified");
            throw new JsonResponseException("operator.username.can.not.be.null");
        }
        String pw = Params.trimToNull(operator.getPassword());
        if (pw == null) {
            log.warn("create operator failed, no password specified");
            throw new JsonResponseException("operator.password.can.not.be.null");
        }

        judgePassword(operator.getPassword());
        judgeUsername(operator.getUsername());

        Operator toCreateOperator = new Operator();
        toCreateOperator.setUserName(operator.getUsername());
        toCreateOperator.setPassword(operator.getPassword());
        toCreateOperator.setRoleId(operator.getRoleId());


        //调用用户中心创建用户
        Response<UcUserInfo> ucUserInfoRes = ucUserOperationLogic.createUcUser(operator.getUsername(),operator.getPassword());
        if(!ucUserInfoRes.isSuccess()){
            log.error("create user center user(name:{}) fail,error:{}",operator.getUsername(),ucUserInfoRes.getError());
            throw new JsonResponseException(ucUserInfoRes.getError());
        }
        UcUserInfo ucUserInfo = ucUserInfoRes.getResult();


        // 创建 middle user
        User user = new User();
        user.setName(un);
        user.setOutId(ucUserInfo.getUserId());
        user.setType(UserType.OPERATOR.value());
        user.setRoles(Lists.newArrayList(UserRole.OPERATOR.name()));
        Response<Long> userCreateResp = userWriteService.create(user);
        if (!userCreateResp.isSuccess()) {
            log.error("failed to create operator user = {}, cause: {}", user, userCreateResp.getError());
            throw new JsonResponseException(userCreateResp.getError());
        }

        // 创建operator
        Long userId = userCreateResp.getResult();
        toCreateOperator.setUserId(userId);

        return RespHelper.or500(operatorWriteService.create(toCreateOperator));
    }

    @RequestMapping(value = "/{userId}", method = RequestMethod.PUT)
    public Boolean updateOperator(@PathVariable Long userId, @RequestBody OperatorPost operator) {

        Response<User> userRes = userReadService.findById(userId);
        if(!userRes.isSuccess()){
            log.error("find user(id:{}) fail,error:{}",userId,userRes.getError());
            throw new JsonResponseException(userRes.getError());
        }
        User existUser = userRes.getResult();


        Response<Operator> operatorResp = operatorReadService.findByUserId(userId);
        if (!operatorResp.isSuccess()) {
            log.warn("operator find fail, userId={}, error={}", userId, operatorResp.getError());
            throw new JsonResponseException(operatorResp.getError());
        }
        Operator existOp = operatorResp.getResult();

        User toUpdateUser = new User();
        toUpdateUser.setId(userId);
        String username = Params.trimToNull(operator.getUsername());

        if (username != null) {
            judgeUsername(username);
            toUpdateUser.setName(username);
        }

        String password = Params.trimToNull(operator.getPassword());
        if (password != null) {
            judgePassword(password);
        }

        //更新用户中心用户信息
        Response<UcUserInfo> ucUserInfoRes = ucUserOperationLogic.updateUcUser(existUser.getOutId(),operator.getUsername(),operator.getPassword());
        if(!ucUserInfoRes.isSuccess()){
            log.error("update user center user(id:{}) fail,error:{}",existUser.getOutId(),ucUserInfoRes.getError());
            throw new JsonResponseException(ucUserInfoRes.getError());
        }

        Response<Boolean> userResp = userWriteService.update(toUpdateUser);
        if (!userResp.isSuccess()) {
            log.warn("user update failed, cause:{}", userResp.getError());
            throw new JsonResponseException(userResp.getError());
        }

        Operator toUpdateOperator = new Operator();
        toUpdateOperator.setId(existOp.getId());
        toUpdateOperator.setUserName(toUpdateUser.getName());
        toUpdateOperator.setRoleId(operator.getRoleId());
        Response<Boolean> opUpdateResp = operatorWriteService.update(toUpdateOperator);
        if (!opUpdateResp.isSuccess()) {
            log.warn("operator update failed, error={}", opUpdateResp.getError());
            throw new JsonResponseException(opUpdateResp.getError());
        }
        return Boolean.TRUE;
    }

    private void judgePassword(String password) {
        if (!password.matches("[\\s\\S]{6,16}")) {
            throw new JsonResponseException(400, "user.password.invalid");
        }
    }

    private void judgeUsername(String username) {
        if (CharMatcher.digit().matchesAllOf(username)
                || CharMatcher.is('@').matchesAnyOf(username)) {
            throw new JsonResponseException(400, "user.username.invalid");
        }
    }

    @RequestMapping(value = "/{userId}/frozen", method = RequestMethod.PUT)
    public Boolean frozenOperator(@PathVariable Long userId) {
        Response<Operator> opResp = operatorReadService.findByUserId(userId);
        if (!opResp.isSuccess()) {
            log.warn("operator find failed, userId={}, error={}", userId, opResp.getError());
            throw new JsonResponseException(opResp.getError());
        }
        Operator op = opResp.getResult();
        if (!op.isActive()) {
            return Boolean.FALSE;
        }

        Operator toUpdate = new Operator();
        toUpdate.setId(op.getId());
        toUpdate.setStatus(0);
        Response<Boolean> updateResp = operatorWriteService.update(toUpdate);
        if (!updateResp.isSuccess()) {
            log.warn("frozen operator failed, userId={}, error={}", userId, updateResp.getError());
            throw new JsonResponseException(updateResp.getError());
        }
        return Boolean.TRUE;
    }

    @RequestMapping(value = "/{userId}/unfrozen", method = RequestMethod.PUT)
    public Boolean unfrozenOperator(@PathVariable Long userId) {
        Response<Operator> opResp = operatorReadService.findByUserId(userId);
        if (!opResp.isSuccess()) {
            log.warn("operator find failed, userId={}, error={}", userId, opResp.getError());
            throw new JsonResponseException(opResp.getError());
        }
        Operator op = opResp.getResult();
        if (!Objects.equals(op.getStatus(), 0)) {
            return Boolean.FALSE;
        }

        Operator toUpdate = new Operator();
        toUpdate.setId(op.getId());
        toUpdate.setStatus(1);
        Response<Boolean> updateResp = operatorWriteService.update(toUpdate);
        if (!updateResp.isSuccess()) {
            log.warn("frozen operator failed, userId={}, error={}", userId, updateResp.getError());
            throw new JsonResponseException(updateResp.getError());
        }
        return Boolean.TRUE;
    }

    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Paging<Operator>> pagingOperator(@RequestParam(required = false) Long roleId,
                                                     @RequestParam(required = false) Integer pageNo,
                                                     @RequestParam(required = false) Integer pageSize) {
        return operatorReadService.pagination(roleId, null, pageNo, pageSize);
    }

    @Data
    public static class OperatorPost {

        private String username;

        private String password;

        private Long roleId;
    }
}
