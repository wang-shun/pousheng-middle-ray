package com.pousheng.middle.common.utils.component;

import com.google.common.base.Throwables;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.*;
import io.terminus.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Created by sunbo@terminus.io on 2017/7/24.
 */
@Slf4j
public class AzureOSSBlobClient {

    @Autowired
    private CloudStorageAccount account;
    @Autowired
    private AzureOSSAutoConfiguration.AzureOSSProperties ossProperties;

    /**
     * 上传至Azure云存储
     *
     * @param file local file need to upload
     * @param path azure container name
     * @return azure file url
     */
    public String upload(File file, String path) {

        if (!file.exists() || !file.canRead() || file.isDirectory()) {
            throw new ServiceException("azure.oss.upload.file.empty");
        }

        try {
            CloudBlockBlob blob = getBlob(path, file.getName());
            blob.uploadFromFile(file.getPath());
            return blob.getUri().toString();
        } catch (URISyntaxException | StorageException | IOException e) {
            log.error("read file fail,file:{},cause:{}", file.getPath(), Throwables.getStackTraceAsString(e));
            throw new ServiceException("export.read.temp.file.fail");
        }
    }

    /**
     * 上传至Azure云存储
     *
     * @param payload content need to upload
     * @param name    azure file name
     * @param path    azure container name
     * @return azure file url
     */
    public String upload(byte[] payload, String name, String path) {

        if (null == payload || payload.length == 0) {
            throw new ServiceException("azure.oss.upload.content.empty");
        }
        try {
            CloudBlockBlob blob = getBlob(path, name);
            upload(payload, blob);
            return blob.getUri().toString();

        } catch (URISyntaxException | StorageException | IOException e) {
            log.error("upload to azure fail,cause:{}", Throwables.getStackTraceAsString(e));
            throw new ServiceException("azure.oss.upload.fail");
        }
    }

    private CloudBlockBlob getBlob(String containerName, String blobName) throws URISyntaxException, StorageException {
        CloudBlobClient client = account.createCloudBlobClient();
        if (null != ossProperties.getTimeout())
            client.getDefaultRequestOptions().setMaximumExecutionTimeInMs(ossProperties.getTimeout());
        CloudBlobContainer container = client.getContainerReference(containerName);

        setUpContainer(container);

        return container.getBlockBlobReference(blobName);
    }

    private void upload(byte[] payload, CloudBlockBlob blob) throws IOException, StorageException {
        if (ossProperties.getMultiThreadingUploadThreshold() != null
                && payload.length > ossProperties.getMultiThreadingUploadThreshold().intValue()) {

        } else {
            blob.uploadFromByteArray(payload, 0, payload.length);
        }
    }

    /**
     * 对container设置允许外部读取
     *
     * @param container
     * @throws StorageException
     */
    private void setUpContainer(CloudBlobContainer container) throws StorageException {
        if (!container.exists()) {
            container.create();
            BlobContainerPermissions permissions = new BlobContainerPermissions();
            permissions.setPublicAccess(BlobContainerPublicAccessType.CONTAINER);
            container.uploadPermissions(permissions);
        }
    }
}
