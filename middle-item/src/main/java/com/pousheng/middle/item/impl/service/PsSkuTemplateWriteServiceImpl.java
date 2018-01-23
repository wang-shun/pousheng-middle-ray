package com.pousheng.middle.item.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.item.impl.dao.SkuTemplateExtDao;
import com.pousheng.middle.item.service.PsSkuTemplateWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.parana.spu.impl.dao.SkuTemplateDao;
import io.terminus.parana.spu.model.SkuTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by songrenfei on 2017/12/7
 */
@Slf4j
@Component
@RpcProvider
@SuppressWarnings("unused")
public class PsSkuTemplateWriteServiceImpl implements PsSkuTemplateWriteService {
    @Autowired
    private SkuTemplateDao skuTemplateDao;
    @Autowired
    private SkuTemplateExtDao skuTemplateExtDao;

    @Override
    public Response<Boolean> update(SkuTemplate skuTemplate) {
        Long skuTemplateId = skuTemplate.getId();
        try {
            SkuTemplate existed = skuTemplateDao.findById(skuTemplateId);
            if(existed == null){
                log.error("SkuTemplate(id={}) not exist", skuTemplateId);
                return Response.fail("sku.template.not.found");
            }

            skuTemplateDao.update(skuTemplate);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("failed to update skuTemplate:{}, cause:{}", skuTemplate, Throwables.getStackTraceAsString(e));
            return Response.fail("sku.template.update.fail");
        }
    }

    @Override
    public Response<Boolean> updateImageByIds(List<Long> ids, String imageUrl) {
        try {
            Boolean isUpdateSuccess = skuTemplateExtDao.updateImageByIds(ids,imageUrl);
            if(isUpdateSuccess){
                return Response.ok(isUpdateSuccess);
            }else {
                return Response.fail("sku.template.update.fail");
            }
        } catch (Exception e) {
            log.error("failed to update skuTemplate:(ids:{}) image to:{}, cause:{}", ids,imageUrl, Throwables.getStackTraceAsString(e));
            return Response.fail("sku.template.update.fail");
        }
    }
}
