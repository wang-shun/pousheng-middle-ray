package com.pousheng.middle;

import com.pousheng.erp.ErpConfiguration;
import com.pousheng.erp.component.SpuInfoFetcher;
import com.pousheng.erp.model.PoushengMaterial;
import io.terminus.parana.ItemApiConfiguration;
import io.terminus.parana.ItemAutoConfig;
//import io.terminus.parana.TradeApiConfig;
//import io.terminus.parana.TradeAutoConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Date;
import java.util.List;

/**
 * Description: add something here
 * User: xiao
 * Date: 28/04/2017
 */
@Configuration
@Import({ItemAutoConfig.class,
        ErpConfiguration.class,
        ItemApiConfiguration.class,
        /*TradeApiConfig.class,
        TradeAutoConfig.class*/})
@EnableScheduling
public class MiddleConfiguration {

    @Bean
    public SpuInfoFetcher<PoushengMaterial> spuInfoFetcher(){
        return new SpuInfoFetcher<PoushengMaterial>() {
            public List<PoushengMaterial> fetch(int pageNo, int pageSize, Date start, Date end) {
                return null;
            }
        };
    }

}
