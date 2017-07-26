package com.pousheng.middle.web.utils.export;

import com.microsoft.azure.storage.CloudStorageAccount;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by sunbo@terminus.io on 2017/7/26.
 */
@Configuration
@EnableConfigurationProperties(AzureOSSAutoConfiguration.AzureOSSProperties.class)
@ConditionalOnClass(name = "com.microsoft.azure.storage.CloudStorageAccount")
public class AzureOSSAutoConfiguration {


    @Bean
    @ConditionalOnMissingBean
    public AzureOSSAccountFactory azureOSSClientFactory(AzureOSSProperties properties) {
        AzureOSSAccountFactory factory = new AzureOSSAccountFactory();
        factory.setProperties(properties);
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean
    public AzureOSSBlobClient azureOSSBlobClient() {
        return new AzureOSSBlobClient();
    }


    class AzureOSSAccountFactory implements FactoryBean<CloudStorageAccount> {

        @Getter
        @Setter
        private AzureOSSProperties properties;

        @Override
        public CloudStorageAccount getObject() throws Exception {
            return CloudStorageAccount.parse("DefaultEndpointsProtocol="
                    + properties.getDefaultEndpointsProtocol()
                    + ";AccountName="
                    + properties.getAccountName()
                    + ";AccountKey="
                    + properties.getAccountKey()
                    + ";EndpointSuffix="
                    + properties.getEndpointSuffix());
        }

        @Override
        public Class<?> getObjectType() {
            return CloudStorageAccount.class;
        }

        @Override
        public boolean isSingleton() {
            return true;
        }
    }

    @Data
    @ConfigurationProperties(prefix = "azure.oss")
    class AzureOSSProperties {
        private String defaultEndpointsProtocol;
        private String accountName;
        private String accountKey;
        private String endpointSuffix;
    }
}
