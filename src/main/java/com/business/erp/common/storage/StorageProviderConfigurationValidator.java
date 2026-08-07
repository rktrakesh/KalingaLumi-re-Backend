package com.business.erp.common.storage;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component public class StorageProviderConfigurationValidator {
 private final String provider;
 public StorageProviderConfigurationValidator(@Value("${app.storage.provider:local}") String provider){this.provider=provider;}
 @PostConstruct void validate(){ if(!"local".equalsIgnoreCase(provider)) throw new IllegalStateException("Unsupported app.storage.provider '" + provider + "'. Supported providers: local"); }
}
