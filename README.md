# ithenticate
ithenticate download similarity report

ithenticate API (The API is based on the XML-RPC specification) has methods for submitting documents and a Get Similarity Report method which returns a set of urls to report data for a document section but it this API you do not have the ability to download a similarity report programmatically (the actual pdf). 
By observing the calls in their website I was able to download the similarity report by using rest web services.
  
The framework that was used is Spring 3.

I have created two classes

The class PlagiarismIthenticateAnswer is used to parse the answer from the rest web services.

The class FetchSimilarityReport contains the code that gets the actual pdf file.

Since, we are not using the XML-RPC API we need to login.

By making a post request (MediaType.APPLICATION_FORM_URLENCODED) to the login url using your password and username you are able to login.

When you login in ithenticate website and you press the "Download Pdf of current report" you will be able to see these next calls in the network (in the browser)

First call (reportId is an actual report id)  <br>
Request URL:https://app.ithenticate.com/paper/{reportId}/queue_pdf?&lang=en_us&output=json   <br>
Request Method:POST   <br>
Request Payload   <br>
{"as":1,"or_type":"similarity"}

with response json that looks like this (uuid is a unique identifier) <br>
{"ready":0,"url":"https://app.ithenticate.com/en_us/dv/print_status/{uuid}"}

Second call <br>
In the second web service call the url returned by the previous rest web service is used namely "https://app.ithenticate.com/en_us/dv/print_status/{uuid}" 

Request URL:https://app.ithenticate.com/en_us/dv/print_status/{uuid}?&lang=en_us&output=json <br>
Request Method:GET <br>

the response looks like this <br>
{"url":"","ready":0}

This call is repeated until the response is ready=1 and an actual url is returned

So the reponse of the last call of the url https://app.ithenticate.com/en_us/dv/print_status/{uuid}
looks like this <br>
{"url":"https://app.ithenticate.com/en_us/dv/print_pdf/{uuid2}","ready":1} <br><br>
Using the above url (https://app.ithenticate.com/en_us/dv/print_pdf/{uuid2}) you would be able to download the pdf 

So in order to make the first call (POST request with as:1 and or_type="similarity") the following code was used

    MultiValueMap<String, String> newMap = new LinkedMultiValueMap<String, String>();
	newMap.add("as", ONE);
	newMap.add("or_type", SIMILARITY);
		
	HttpEntity<MultiValueMap<String, String>> newRequest = new HttpEntity<MultiValueMap<String, String>>(newMap, headers);
		
	ResponseEntity<PlagiarismIthenticateAnswer>  queuePdfResult = restTemplate.postForEntity("https://app.ithenticate.com/paper/" + reportId + "/queue_pdf?&lang=en_us&output=json", newRequest, PlagiarismIthenticateAnswer.class);

	String urlPrintStatus = queuePdfResult.getBody().getUrl();
  
  
 After the url is returned from the above call, this url is used in the subsequent web sercice calls until the field "ready" equals 1
 
 So, this code is used to loop until a successful call is returned
 
    
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
    
When the successful response is returned (the field ready=1 and there is an actual url returned), the url that is returned from the above code is used to fetch the pdf document
 
The code to download the pdf follows
 

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
    
Hope this helps someone.
