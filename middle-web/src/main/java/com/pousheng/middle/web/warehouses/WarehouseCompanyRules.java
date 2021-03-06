package com.pousheng.middle.web.warehouses;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.warehouse.model.WarehouseCompanyRule;
import com.pousheng.middle.warehouse.service.WarehouseCompanyRuleReadService;
import com.pousheng.middle.warehouse.service.WarehouseCompanyRuleWriteService;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.utils.operationlog.OperationLogModule;
import com.pousheng.middle.web.warehouses.dto.Company;
import com.pousheng.middle.web.warehouses.dto.CompanyRuleDto;
import com.pousheng.middle.web.warehouses.dto.ErpShop;
import io.swagger.annotations.ApiOperation;
import io.terminus.applog.annotation.LogMe;
import io.terminus.applog.annotation.LogMeContext;
import io.terminus.applog.annotation.LogMeId;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.BeanMapper;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-21
 */
@RestController
@RequestMapping("/api/warehouse/company-rule")
@Slf4j
@OperationLogModule(OperationLogModule.Module.WAREHOUSE_COMPANY_RULE)
public class WarehouseCompanyRules {

    public static final ObjectMapper mapper = JsonMapper.nonEmptyMapper().getMapper();

    @Value("${gateway.member.host}")
    private String memberHost;

    @RpcConsumer
    private WarehouseCompanyRuleReadService warehouseCompanyRuleReadService;

    @RpcConsumer
    private WarehouseCompanyRuleWriteService warehouseCompanyRuleWriteService;

    @Autowired
    private WarehouseCacher warehouseCacher;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;



    @ApiOperation("创建")
    @LogMe(description = "创建公司规则", compareTo = "warehouseCompanyRuleDao#findById")
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long create(@RequestBody @LogMeContext WarehouseCompanyRule warehouseCompanyRule){
        String warehouseCompanyRuleStr = JsonMapper.nonEmptyMapper().toJson(warehouseCompanyRule);
        if(log.isDebugEnabled()){
            log.debug("API-WAREHOUSE-COMPANY-RULE-CREATE-START param: warehouseCompanyRule [{}]",warehouseCompanyRuleStr);
        }
        Long warehouseId = warehouseCompanyRule.getWarehouseId();
        WarehouseDTO warehouse = warehouseCacher.findById(warehouseId);
        if(!Objects.equal(warehouse.getCompanyCode(), warehouseCompanyRule.getCompanyCode())){
            log.error("company code mismatch, expect: {}, actual:{}",warehouseCompanyRule.getCompanyCode(),
                    warehouse.getCompanyCode() );
            throw new JsonResponseException("company.code.mismatch");
        }
        Response<Long> r = warehouseCompanyRuleWriteService.create(warehouseCompanyRule);
        if(!r.isSuccess()){
            log.error("failed to create {}, error code:{}", warehouseCompanyRule, r.getError());
            throw new JsonResponseException(r.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-WAREHOUSE-COMPANY-RULE-CREATE-END param: warehouseCompanyRule [{}] ,resp: [{}]",warehouseCompanyRuleStr,r.getResult());
        }
        return r.getResult();
    }

    @ApiOperation("更新")
    @LogMe(description = "更新公司规则", compareTo = "warehouseCompanyRuleDao#findById")
    @RequestMapping(method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean update(@RequestBody @LogMeContext WarehouseCompanyRule warehouseCompanyRule){
        String warehouseCompanyRuleStr = JsonMapper.nonEmptyMapper().toJson(warehouseCompanyRule);
        if(log.isDebugEnabled()){
            log.debug("API-WAREHOUSE-COMPANY-RULE-UPDATE-START param: warehouseCompanyRule [{}]",warehouseCompanyRuleStr);
        }
        Long warehouseId = warehouseCompanyRule.getWarehouseId();
        WarehouseDTO warehouse = warehouseCacher.findById(warehouseId);
        if(!Objects.equal(warehouse.getCompanyCode(), warehouseCompanyRule.getCompanyCode())){
            log.error("company code mismatch, expect: {}, actual:{}",warehouseCompanyRule.getCompanyCode(),
                    warehouse.getCompanyCode() );
            throw new JsonResponseException("company.code.mismatch");
        }
        Response<Boolean> r = warehouseCompanyRuleWriteService.update(warehouseCompanyRule);
        if(!r.isSuccess()){
            log.error("failed to update {}, error code:{}", warehouseCompanyRule, r.getError());
            throw new JsonResponseException(r.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-WAREHOUSE-COMPANY-RULE-UPDATE-END param: warehouseCompanyRule [{}] ,resp: [{}]",warehouseCompanyRuleStr,r.getResult());
        }
        return r.getResult();
    }

    @ApiOperation("删除")
    @LogMe(description = "删除公司规则", compareTo = "warehouseCompanyRuleDao#findById")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean delete(@PathVariable @LogMeId Long id){
        if(log.isDebugEnabled()){
            log.debug("API-WAREHOUSE-COMPANY-RULE-DELETE-START param: id [{}]",id);
        }
        Response<Boolean> r = warehouseCompanyRuleWriteService.deleteById(id);
        if(!r.isSuccess()){
            log.error("failed to delete WarehouseCompanyRule(id={}), error code:{}", id, r.getError());
            throw new JsonResponseException(r.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-WAREHOUSE-COMPANY-RULE-DELETE-END param: id [{}] ,resp: [{}]",id,r.getResult());
        }
        return r.getResult();
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public WarehouseCompanyRule findById(@PathVariable Long id){
        if(log.isDebugEnabled()){
            log.debug("API-WAREHOUSE-COMPANY-RULE-FINDBYID-START param: id [{}]",id);
        }
        Response<WarehouseCompanyRule> r = warehouseCompanyRuleReadService.findById(id);
        if(!r.isSuccess()){
            log.error("failed to find WarehouseCompanyRule(id={}), error code:{}", id, r.getError());
            throw new JsonResponseException(r.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-WAREHOUSE-COMPANY-RULE-FINDBYID-END param: id [{}] ,resp: [{}]",id,JsonMapper.nonEmptyMapper().toJson(r.getResult()));
        }
        return r.getResult();
    }

    @RequestMapping(value = "/paging",method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<CompanyRuleDto> pagination(@RequestParam(required = false, value = "pageNo") Integer pageNo,
                                             @RequestParam(required = false, value = "pageSize") Integer pageSize,
                                             @RequestParam(required = false, value="companyCode")String companyCode){
        if(log.isDebugEnabled()){
            log.debug("API-WAREHOUSE-COMPANY-RULE-PAGINATION-START param: pageNo [{}] pageSize [{}] companyCode [{}]",pageNo,pageSize,companyCode);
        }
        Map<String, Object> params = Maps.newHashMapWithExpectedSize(3);
        if(StringUtils.hasText(companyCode)){
            params.put("companyCode", companyCode);
        }
        Response<Paging<WarehouseCompanyRule>> r = warehouseCompanyRuleReadService.pagination(pageNo, pageSize, params);
        if(!r.isSuccess()){
            log.error("failed to pagination WarehouseCompanyRule, params:{}, error code:{}", params, r.getError());
            throw new JsonResponseException(r.getError());
        }

        Paging<CompanyRuleDto> result = new Paging<>();
        Paging<WarehouseCompanyRule> rules = r.getResult();
        result.setTotal(rules.getTotal());
        List<CompanyRuleDto> crds = Lists.newArrayList();
        for (WarehouseCompanyRule warehouseCompanyRule : rules.getData()) {
            CompanyRuleDto crd = new CompanyRuleDto();
            BeanMapper.copy(warehouseCompanyRule, crd);
            Long warehosueId = crd.getWarehouseId();
            WarehouseDTO warehouse = warehouseCacher.findById(warehosueId);
            crd.setWarehouseCode(warehouse.getInnerCode());
            crds.add(crd);
        }
        result.setData(crds);
        if(log.isDebugEnabled()){
            log.debug("API-WAREHOUSE-COMPANY-RULE-PAGINATION-END param: pageNo [{}] pageSize [{}] companyCode [{}] ,resp: [{}]",pageNo,pageSize,companyCode,JsonMapper.nonEmptyMapper().toJson(result));
        }
        return result;
    }

    /**
     * 列出当前未设置规则的公司
     *
     * @return 当前未设置规则的公司列表
     */
    @RequestMapping(value = "/company-candidate", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Company> companyCandidate(){
        if(log.isDebugEnabled()){
            log.debug("API-WAREHOUSE-COMPANY-RULE-COMPANYCANDIDATE-START noparam: ");
        }
        Map<String, Integer> params = ImmutableMap.of("pageNo",1, "pageSize", Integer.MAX_VALUE);
        HttpRequest r = HttpRequest.get(memberHost+"/api/member/pousheng/company/list", params, true)
                .acceptJson()
                .acceptCharset(HttpRequest.CHARSET_UTF8);
        if (r.ok()) {
            if(log.isDebugEnabled()){
               log.debug("API-WAREHOUSE-COMPANY-RULE-COMPANYCANDIDATE-END noparam: ,resp: [{}]",JsonMapper.nonEmptyMapper().toJson(r.body()));
            }
            return todoCompanies(r.body());

        } else {
            int code = r.code();
            String body = r.body();
            log.error("failed to get company list (params:{}), http code:{}, message:{}",
                    params, code, body);
            throw new JsonResponseException("member.company.request.fail");
        }

    }

    @RequestMapping(value = "/erpShops", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ErpShop> erpShops(@RequestParam("companyCode")String companyCode,
                                  @RequestParam(value = "prefix", required = false) String namePrefix){
        if(log.isDebugEnabled()){
            log.debug("API-WAREHOUSE-COMPANY-RULE-ERPSHOPS-START param: companyCode [{}] namePrefix [{}]",companyCode,namePrefix);
        }
        Map<String, Object> params = Maps.newHashMap();
        params.put("pageNo", 1);
        params.put("pageSize", 10);
        params.put("storeType", 1);
        params.put("companyId", companyCode);
        if(StringUtils.hasText(namePrefix)){
            params.put("storeName", namePrefix);
        }
        HttpRequest r = HttpRequest.get(memberHost+"/api/member/pousheng/shop/list", params, true)
                .acceptJson()
                .acceptCharset(HttpRequest.CHARSET_UTF8);
        if (r.ok()) {
            String body = r.body();
            try {
                JsonNode root = mapper.readTree(body);
                return mapper.readValue(root.findPath("data").toString(), new TypeReference<List<ErpShop>>() {
                });
            }catch (Exception e){
                log.error("failed to deserialize shop list from member center, body:{}, cause:{} ",
                        body,  Throwables.getStackTraceAsString(e));
                throw new JsonResponseException("member.shop.request.fail");
            }

        } else {
            int code = r.code();
            String body = r.body();
            log.error("failed to get shop list (params:{}), http code:{}, message:{}",
                    params, code, body);
            throw new JsonResponseException("member.shop.request.fail");
        }
    }

    private List<Company> todoCompanies(String body) {
        try {
            JsonNode root = mapper.readTree(body);
            List<Company> all = mapper.readValue(root.findPath("data").toString(), new TypeReference<List<Company>>() {
            });

            //过滤掉已设置规则的公司
            Response<List<String>> rCompanyCodes = warehouseCompanyRuleReadService.findCompanyCodes();
            if(!rCompanyCodes.isSuccess()){
                log.error("failed to find company codes where rule set");
                throw new JsonResponseException("company.rule.request.fail");
            }
            Set<String> doneCompanyCodes = Sets.newHashSet(rCompanyCodes.getResult());
            List<Company> todoCompanies = Lists.newArrayList();
            for (Company company : all) {
                if(!doneCompanyCodes.contains(company.getId())){
                    todoCompanies.add(company);
                }
            }
            return todoCompanies;
        } catch (Exception e) {
            log.error("failed to find companies to set rules for {}, cause:{}", body, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("company.rule.request.fail");
        }
    }



    @RequestMapping(value = "/by/warehouse-code", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public WarehouseCompanyRule findByWarehouseCode(@RequestParam String warehouseCode){
        if(log.isDebugEnabled()){
            log.debug("API-WAREHOUSE-COMPANY-RULE-FINDBYWAREHOUSECODE-START param: warehouseCode [{}] ",warehouseCode);
        }
        Response<WarehouseCompanyRule> r = shipmentReadLogic.findCompanyRuleByWarehouseCode(warehouseCode);
        if(!r.isSuccess()){
            log.error("failed to find WarehouseCompanyRule by warehouseCode={}, error code:{}", warehouseCode, r.getError());
            throw new JsonResponseException(r.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-WAREHOUSE-COMPANY-RULE-FINDBYWAREHOUSECODE-END param: warehouseCode [{}] ,resp: [{}]",warehouseCode,JsonMapper.nonEmptyMapper().toJson(r.getResult()));
        }
        return r.getResult();
    }



}
