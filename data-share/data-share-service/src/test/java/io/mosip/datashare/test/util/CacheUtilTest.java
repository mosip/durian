package io.mosip.datashare.test.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import io.mosip.datashare.util.CacheUtil;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "org.w3c.dom.*",
		"com.sun.org.apache.xalan.*" })
public class CacheUtilTest {

	@InjectMocks
	CacheUtil cacheUtil;

	@Test
	public void cacheSuccessTest() throws IOException {

		assertNotNull(cacheUtil.getShortUrlData("shortkey", "policyId", "subscriberId", "randomShareKey"));


	}

	@Test
	public void cacheFailureTest() throws IOException {

		assertNull(cacheUtil.getShortUrlData("shortkey", null, "subscriberId", "randomShareKey"));

	}
}
