package com.pousheng.middle.web;

import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.pousheng.auth.AuthConfiguration;
import com.pousheng.erp.ErpConfiguration;
import com.pousheng.erp.model.PoushengMaterial;
import com.pousheng.middle.PoushengMiddleItemConfiguration;
import com.pousheng.middle.interceptors.LoginInterceptor;
import com.pousheng.middle.web.converters.PoushengJsonMessageConverter;
import io.terminus.open.client.center.OpenClientCenterAutoConfig;
import io.terminus.open.client.parana.ParanaAutoConfiguration;
import io.terminus.parana.ItemApiConfiguration;
import io.terminus.parana.TradeApiConfig;
import io.terminus.parana.TradeAutoConfig;
import io.terminus.parana.auth.AuthApiConfiguration;
import io.terminus.parana.order.api.DeliveryFeeCharger;
import io.terminus.parana.order.dto.RichSkusByShop;
import io.terminus.parana.order.model.ReceiverInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Description: add something here
 * MiddleUser: xiao
 * Date: 28/04/2017
 */
@Configuration
@Import({PoushengMiddleItemConfiguration.class,
        ErpConfiguration.class,
        ItemApiConfiguration.class,
        TradeApiConfig.class,
        TradeAutoConfig.class,
        ParanaAutoConfiguration.class,
        AuthConfiguration.class,
        AuthApiConfiguration.class,
        OpenClientCenterAutoConfig.class})

@ComponentScan(
        {"com.pousheng.middle.order",
                "com.pousheng.middle.warehouse",
                "com.pousheng.middle.open",
                "com.pousheng.middle.advices",
                "com.pousheng.middle.auth",
                "com.pousheng.middle.erpsyc",
                "com.pousheng.middle.interceptors",
                "com.pousheng.middle.web"})
@EnableScheduling
public class MiddleConfiguration extends WebMvcConfigurerAdapter {


    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor);
    }


    /**
     * 中台不需要计算运费
     *
     * @return deliveryFeeCharger
     */
    @Bean
    public DeliveryFeeCharger deliveryFeeCharger() {
        return new DeliveryFeeCharger() {
            @Override
            public Integer charge(Long aLong, Integer integer, Integer integer1) {
                return 0;
            }

            @Override
            public Map<Long, Integer> charge(List<RichSkusByShop> list, ReceiverInfo receiverInfo) {
                return Collections.emptyMap();
            }
        };
    }

    @Bean
    public EventBus eventBus() {
        return new AsyncEventBus(
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2));
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatter(new DateFormatter("yyyy-MM-dd HH:mm:ss"));
        registry.addFormatter(new DateFormatter("yyyy-MM-dd"));
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        Iterables.removeIf(converters, new Predicate<HttpMessageConverter<?>>() {
            @Override
            public boolean apply(HttpMessageConverter<?> input) {
                if (input instanceof AbstractJackson2HttpMessageConverter) {
                    return true;
                }
                if (input instanceof StringHttpMessageConverter) {
                    return true;
                }
                return false;
            }
        });

        //force utf-8 encode
        StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter(Charsets.UTF_8);
        stringHttpMessageConverter.setSupportedMediaTypes(Lists.newArrayList(MediaType.TEXT_PLAIN, MediaType.ALL));
        converters.add(1, stringHttpMessageConverter);

        final PoushengJsonMessageConverter paranaJsonMessageConverter = new PoushengJsonMessageConverter();
        //paranaJsonMessageConverter.setObjectMapper(JsonMapper.nonEmptyMapper().getMapper());
        converters.add(paranaJsonMessageConverter);
    }



    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("middle_messages", "messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(true);
        messageSource.setCacheSeconds(-1);
        return messageSource;
    }

}