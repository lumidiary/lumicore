package com.example.lumicore.config;


import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.transfer.UploadConfiguration;
import com.oracle.bmc.objectstorage.transfer.UploadManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OCI Object Storage 클라이언트 및 UploadManager 빈 등록
 */
@Configuration
@Slf4j
public class OciObjectStorageConfig {

    @Value("${oci.configFilePath}")
    private String configFilePath;

    @Bean
    public ObjectStorage objectStorage() throws Exception {
        // ~/.oci/config 의 DEFAULT 프로파일 읽기
        ConfigFileReader.ConfigFile config = ConfigFileReader.parse(configFilePath, "DEFAULT");
        log.debug("Loaded OCI Config → tenancy={}, user={}, region={}",
                config.get("tenancy"), config.get("user"), config.get("region"));

        AuthenticationDetailsProvider provider =
                new ConfigFileAuthenticationDetailsProvider(config);

        // Config 파일에서 region 읽어와서 사용
        String regionStr = config.get("region");
        Region region = Region.fromRegionId(regionStr);
        
        log.info("OCI ObjectStorage 클라이언트 생성: region={}", region);

        return ObjectStorageClient.builder()
                .region(region)  // ← Config 파일의 region 사용
                .build(provider);
    }

    @Bean
    public UploadManager uploadManager(ObjectStorage objectStorage) {
        // 멀티파트 & 병렬 업로드 허용
        UploadConfiguration configuration = UploadConfiguration.builder()
                .allowMultipartUploads(true)
                .allowParallelUploads(true)
                .build();
        return new UploadManager(objectStorage, configuration);
    }
}