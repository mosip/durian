package io.mosip.datashare.test.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.datashare.dto.DataShare;
import io.mosip.datashare.dto.DataShareDto;
import io.mosip.datashare.dto.PolicyAttributesDto;
import io.mosip.datashare.dto.PolicyResponseDto;
import io.mosip.datashare.exception.DataShareExpiredException;
import io.mosip.datashare.exception.DataShareNotFoundException;
import io.mosip.datashare.exception.FileException;
import io.mosip.datashare.service.impl.DataShareServiceImpl;
import io.mosip.datashare.util.CacheUtil;
import io.mosip.datashare.util.DigitalSignatureUtil;
import io.mosip.datashare.util.EncryptionUtil;
import io.mosip.datashare.util.PolicyUtil;
import io.mosip.kernel.core.util.CryptoUtil;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "org.w3c.dom.*",
		"com.sun.org.apache.xalan.*" })
@PowerMockRunnerDelegate(SpringRunner.class)
@PrepareForTest(value = { RandomStringUtils.class, URL.class, CryptoUtil.class })
public class DataShareServiceImplTest {

	@Mock
	private PolicyUtil policyUtil;

	/** The encryption util. */
	@Mock
	private EncryptionUtil encryptionUtil;

	@Mock
	private CacheUtil cacheUtil;

	/** The env. */
	@Mock
	private Environment env;

	/** The digital signature util. */
	@Mock
	private DigitalSignatureUtil digitalSignatureUtil;

	/** The object store adapter. */
	@Mock
	ObjectStoreAdapter objectStoreAdapter;

	@InjectMocks
	DataShareServiceImpl dataShareServiceImpl;

	private PolicyResponseDto policyResponseDto;

	private byte[] dataBytes;

	private String SUBSCRIBER_ID = "subscriberid";
	
	private String POLICY_ID = "policyid";

	Map<String, Object> metaDataMap;

	MockMultipartFile multiPartFile;

	InputStream inputStream;

	private PolicyAttributesDto policyAttributesDto;
	@Before
	public void setUp() throws Exception {
		ReflectionTestUtils.setField(dataShareServiceImpl, "servletPath", "/");
		ReflectionTestUtils.setField(dataShareServiceImpl, "isShortUrl", false);
		ReflectionTestUtils.setField(dataShareServiceImpl, "httpProtocol", "https");
		PowerMockito.mockStatic(RandomStringUtils.class);
		Mockito.when(RandomStringUtils.randomAlphanumeric(Mockito.anyInt())).thenReturn("dfg3456f");
		metaDataMap = new HashMap<String, Object>();
		metaDataMap.put("transactionsallowed", "2");
		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource("test.txt").getFile());

		inputStream = new FileInputStream(file);
		dataBytes = IOUtils.toByteArray(inputStream);
		multiPartFile = new MockMultipartFile("file", "NameOfTheFile", "multipart/form-data",
				new ByteArrayInputStream(dataBytes));

		policyResponseDto = new PolicyResponseDto();
		DataShareDto dataSharePolicies = new DataShareDto();
		dataSharePolicies.setEncryptionType("partnerBased");
		dataSharePolicies.setShareDomain("dev.mosip.net");
		dataSharePolicies.setTransactionsAllowed("2");
		dataSharePolicies.setValidForInMinutes("60");
		policyAttributesDto = new PolicyAttributesDto();
		policyAttributesDto.setDataSharePolicies(dataSharePolicies);
		policyResponseDto.setPolicies(policyAttributesDto);
		Mockito.when(policyUtil.getPolicyDetail(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(policyResponseDto);
		Mockito.when(encryptionUtil.encryptData(Mockito.any(), Mockito.anyString()))
				.thenReturn(dataBytes);
		
		Mockito.when(digitalSignatureUtil.sign(dataBytes))
		.thenReturn(dataBytes.toString());
		Mockito.when(objectStoreAdapter.putObject(Mockito.anyString(), Mockito.anyString(), Mockito.any(),
				Mockito.any(), Mockito.anyString(),
				Mockito.any())).thenReturn(true);
		Mockito.when(objectStoreAdapter.addObjectMetaData(Mockito.anyString(), Mockito.anyString(), Mockito.any(),
				Mockito.any(), Mockito.anyString(),
				Mockito.any())).thenReturn(metaDataMap);
		URL u = PowerMockito.mock(URL.class);

		PowerMockito.whenNew(URL.class).withArguments(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())
				.thenReturn(u);
		
		Mockito.when(objectStoreAdapter.getMetaData(Mockito.anyString(), Mockito.anyString(), Mockito.any(),
				Mockito.any(), Mockito.anyString()
				)).thenReturn(metaDataMap);
		Mockito.when(objectStoreAdapter.getObject(Mockito.anyString(), Mockito.anyString(), Mockito.any(),
				Mockito.any(), Mockito.anyString()))
				.thenReturn(inputStream);
		Mockito.when(cacheUtil.getShortUrlData(Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any()))
				.thenReturn(POLICY_ID + "," + SUBSCRIBER_ID + "," + "dfg3456f");
	}

	@Test
	public void createDataShareSuccessTest() {

		DataShare dataShare = dataShareServiceImpl.createDataShare(POLICY_ID, SUBSCRIBER_ID, multiPartFile);
		assertEquals("Data Share created successfully", POLICY_ID, dataShare.getPolicyId());
	}

	@Test
	public void createDataShareSuccesswithShortUrlTest() {
		ReflectionTestUtils.setField(dataShareServiceImpl, "isShortUrl", true);
		DataShare dataShare = dataShareServiceImpl.createDataShare(POLICY_ID, SUBSCRIBER_ID, multiPartFile);
		assertEquals("Data Share created successfully", POLICY_ID, dataShare.getPolicyId());
	}

	@Test(expected = FileException.class)
	public void fileExceptionTest() {
		multiPartFile=null;
		dataShareServiceImpl.createDataShare(POLICY_ID, SUBSCRIBER_ID, multiPartFile);
	}


	@Test
	public void getDataFileSuccessTest() {

		assertNotNull(dataShareServiceImpl.getDataFile(POLICY_ID, SUBSCRIBER_ID, "12dfsdff"));
	}

	@Test(expected = DataShareNotFoundException.class)
	public void dataShareNotFoundExceptionTest() {
		Mockito.when(objectStoreAdapter.getObject(Mockito.anyString(), Mockito.anyString(), Mockito.any(),
				Mockito.any(), Mockito.anyString()))
				.thenReturn(null);
		dataShareServiceImpl.getDataFile(POLICY_ID, SUBSCRIBER_ID, "12dfsdff");
	}

	@Test(expected = DataShareExpiredException.class)
	public void dataShareExpiredExceptionTest() {
		metaDataMap.put("transactionsallowed", "0");
		dataShareServiceImpl.getDataFile(POLICY_ID, SUBSCRIBER_ID, "12dfsdff");
	}

	@Test
	public void getDataFileWithShortKeySuccessTest() {
		ReflectionTestUtils.setField(dataShareServiceImpl, "isShortUrl", true);
		assertNotNull(dataShareServiceImpl.getDataFile("12dfsdff"));
	}
}
