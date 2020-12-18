package io.mosip.datashare.test.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

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
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.datashare.dto.JWTSignatureResponseDto;
import io.mosip.datashare.dto.SignResponseDto;
import io.mosip.datashare.exception.SignatureException;
import io.mosip.datashare.util.DigitalSignatureUtil;
import io.mosip.datashare.util.RestUtil;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.HMACUtils2;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "org.w3c.dom.*",
		"com.sun.org.apache.xalan.*" })
@PrepareForTest(value = { CryptoUtil.class, HMACUtils2.class })
public class DigitalSignatureUtilTest {

	/** The environment. */
	@Mock
	private Environment environment;

	/** The rest template. */
	@Mock
	private RestUtil restUtil;

	/** The mapper. */
	@Mock
	private ObjectMapper objectMapper;

	@InjectMocks
	DigitalSignatureUtil digitalSignatureUtil;

	private SignResponseDto signResponseDto;

	String jwtsignResponse;

	String data = "testdata";


	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws JsonParseException, JsonMappingException, IOException, NoSuchAlgorithmException {
		ReflectionTestUtils.setField(digitalSignatureUtil, "digestAlg", "SHA256");
		signResponseDto = new SignResponseDto();
		JWTSignatureResponseDto jwtSign = new JWTSignatureResponseDto();
		jwtSign.setJwtSignedData(data);
		signResponseDto.setResponse(jwtSign);
		jwtsignResponse = "{\r\n" +
    		"  \"id\": \"string\",\r\n" + 
    		"  \"version\": \"string\",\r\n" + 
    		"  \"responsetime\": \"2020-07-28T10:06:31.530Z\",\r\n" + 
    		"  \"metadata\": null,\r\n" + 
    		"  \"response\": {\r\n" + 
				"    \"jwtSignedData\": \"testdata\",\r\n" + 
    		"    \"timestamp\": \"2020-07-28T10:06:31.502Z\"\r\n" + 
    		"  },\r\n" + 
    		"  \"errors\": null\r\n" + 
				"}";

		Mockito.when(restUtil.postApi(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any())).thenReturn(jwtsignResponse);

		Mockito.when(objectMapper.readValue(jwtsignResponse, SignResponseDto.class)).thenReturn(signResponseDto);
		Mockito.when(environment.getProperty("mosip.data.share.datetime.pattern"))
				.thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		Mockito.when(environment.getProperty("mosip.data.share.includeCertificateHash"))
				.thenReturn("false");
		Mockito.when(environment.getProperty("mosip.data.share.includeCertificate")).thenReturn("false");
		Mockito.when(environment.getProperty("mosip.data.share.includePayload")).thenReturn("false");
		PowerMockito.mockStatic(CryptoUtil.class);
		Mockito.when(CryptoUtil.encodeBase64(Mockito.any())).thenReturn(data);
		PowerMockito.mockStatic(HMACUtils2.class);

		Mockito.when(HMACUtils2.digestAsPlainText(Mockito.any())).thenReturn(data);
		Mockito.when(objectMapper.writeValueAsString(Mockito.any()))
				.thenReturn(data);
	}

	@Test
	public void signSuccessTest() throws IOException {
		String test = "testdata";
		byte[] sample = test.getBytes();
		
		String signedData = digitalSignatureUtil.jwtSign(sample, "test", "", "", "");
		assertEquals(test, signedData);
		

	}

	@Test(expected = SignatureException.class)
	public void testIOException() throws JsonParseException, JsonMappingException, IOException {
		String test = "testdata";
		byte[] sample = test.getBytes();
		Mockito.when(objectMapper.readValue(jwtsignResponse, SignResponseDto.class)).thenThrow(new IOException());
		String signedData = digitalSignatureUtil.jwtSign(sample, "test", "", "", "");
	}




	@Test(expected = SignatureException.class)
	public void signFailureTest() throws JsonParseException, JsonMappingException, IOException {
		ServiceError error = new ServiceError();
		error.setErrorCode("KER-SIG-001");
		error.setMessage("sign error");
		List<ServiceError> errors = new ArrayList<ServiceError>();
		errors.add(error);
		signResponseDto.setErrors(errors);
		String test = "testdata";
		byte[] sample = test.getBytes();

		String signedData = digitalSignatureUtil.jwtSign(sample, "test", "", "", "");
	}
}
