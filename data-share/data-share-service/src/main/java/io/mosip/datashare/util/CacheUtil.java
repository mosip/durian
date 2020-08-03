package io.mosip.datashare.util;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class CacheUtil {

	@Cacheable(value = "shortdata", key = "#shortRandomShareKey")
	public String getShortUrlData(String shortRandomShareKey, String policyId, String subscriberId,
			String randomShareKey) {
		if (policyId == null || subscriberId == null || randomShareKey == null) {
			return null;
		} else {
			return policyId + "," + subscriberId + "," + randomShareKey;
		}


	}
}
