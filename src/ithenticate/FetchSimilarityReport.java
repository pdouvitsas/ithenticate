package ithenticate;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import ithenticate.PlagiarismIthenticateAnswer;

public class FetchSimilarityReport {

	private static final String USER_NAME = "yourusernameforithenticate";
	private static final String PASSWORD =  "password";
	

	private static final String LOGIN_URL = "https://app.ithenticate.com/en_us/login";
	
	private static final String SIMILARITY = "similarity";
	
	private static final String ONE = "1";
	
	@Override
	public byte[] getSimilarityReportByReportId (Integer reportId) {
		RestTemplate restTemplate = new RestTemplate();
		
		final HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
		final HttpClient httpClient = HttpClientBuilder.create()
		                                               .setRedirectStrategy(new LaxRedirectStrategy())
		                                               .build();
		factory.setHttpClient(httpClient);
		restTemplate.setRequestFactory(factory);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
		map.add("username", USER_NAME);
		map.add("password", PASSWORD);

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);

		//you do this to logon
		ResponseEntity<String> response = restTemplate.postForEntity(LOGIN_URL, request, String.class);
		
		MultiValueMap<String, String> newMap = new LinkedMultiValueMap<String, String>();
		newMap.add("as", ONE);
		newMap.add("or_type", SIMILARITY);
		
		HttpEntity<MultiValueMap<String, String>> newRequest = new HttpEntity<MultiValueMap<String, String>>(newMap, headers);
		
		ResponseEntity<PlagiarismIthenticateAnswer>  queuePdfResult = restTemplate.postForEntity("https://app.ithenticate.com/paper/" + reportId + "/queue_pdf?&lang=en_us&output=json", newRequest, PlagiarismIthenticateAnswer.class);

		String urlPrintStatus = queuePdfResult.getBody().getUrl();
		
		Integer resultReady = 0;
		Integer timesTried = 0;
		PlagiarismIthenticateAnswer printPdfResult = restTemplate.getForObject(urlPrintStatus, PlagiarismIthenticateAnswer.class);
		int counter = 30;
		resultReady = printPdfResult.getReady();
		
		while (resultReady == 0 && timesTried < counter ) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			printPdfResult = restTemplate.getForObject(urlPrintStatus, PlagiarismIthenticateAnswer.class);
			resultReady = printPdfResult.getReady();
			timesTried++;
		}
		
		//download the pdf
		ByteArrayHttpMessageConverter byteArrayHttpMessageConverter = new ByteArrayHttpMessageConverter();

		List<MediaType> supportedApplicationTypes = new ArrayList<MediaType>();
		MediaType pdfApplication = new MediaType("application","pdf");
		supportedApplicationTypes.add(pdfApplication);

		byteArrayHttpMessageConverter.setSupportedMediaTypes(supportedApplicationTypes);
		List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
		messageConverters.add(byteArrayHttpMessageConverter);
		restTemplate.setMessageConverters(messageConverters);
		if (resultReady == 1) {
			String printPdfUrl = printPdfResult.getUrl();
			Object result = restTemplate.getForObject(printPdfUrl, byte[].class);
			byte[] resultByteArr = (byte[])result;
			return resultByteArr;
		} else {
			return null;
		}
	
	}
}
