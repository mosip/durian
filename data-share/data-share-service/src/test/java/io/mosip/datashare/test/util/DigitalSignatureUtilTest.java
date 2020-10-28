package io.mosip.datashare.test.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.datashare.dto.SignResponseDto;
import io.mosip.datashare.dto.SignatureResponse;
import io.mosip.datashare.exception.SignatureException;
import io.mosip.datashare.util.DigitalSignatureUtil;
import io.mosip.datashare.util.RestUtil;
import io.mosip.kernel.core.exception.ServiceError;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "org.w3c.dom.*",
		"com.sun.org.apache.xalan.*" })
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

	String signResponse;


	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws JsonParseException, JsonMappingException, IOException {

		signResponseDto = new SignResponseDto();
		SignatureResponse sign = new SignatureResponse();
		sign.setSignature("testdata");
		signResponseDto.setResponse(sign);
		signResponse = "{\r\n" + 
    		"  \"id\": \"string\",\r\n" + 
    		"  \"version\": \"string\",\r\n" + 
    		"  \"responsetime\": \"2020-07-28T10:06:31.530Z\",\r\n" + 
    		"  \"metadata\": null,\r\n" + 
    		"  \"response\": {\r\n" + 
				"    \"signature\": \"testdata\",\r\n" + 
    		"    \"timestamp\": \"2020-07-28T10:06:31.502Z\"\r\n" + 
    		"  },\r\n" + 
    		"  \"errors\": null\r\n" + 
				"}";

		Mockito.when(restUtil.postApi(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any())).thenReturn(signResponse);

		Mockito.when(objectMapper.readValue(signResponse, SignResponseDto.class)).thenReturn(signResponseDto);
		Mockito.when(environment.getProperty("mosip.data.share.datetime.pattern"))
				.thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	}

	@Test
	public void signSuccessTest() throws IOException {
		String test = "testdata";
		byte[] sample = test.getBytes();
		
		String signedData = digitalSignatureUtil.sign(sample);
		assertEquals(test, signedData);
		

	}

	@Test(expected = SignatureException.class)
	public void testIOException() throws JsonParseException, JsonMappingException, IOException {
		String test = "testdata";
		byte[] sample = test.getBytes();
		Mockito.when(objectMapper.readValue(signResponse, SignResponseDto.class)).thenThrow(new IOException());
		digitalSignatureUtil.sign(sample);
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

		digitalSignatureUtil.sign(sample);
	}
}
