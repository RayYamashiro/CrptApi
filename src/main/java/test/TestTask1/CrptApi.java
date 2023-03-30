package test.TestTask1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


@Data
public class CrptApi {
    //public static void main(String[] args) {
    //	SpringApplication.run(CrptApi.class, args);
    //}

    private final String URL = "https://ismp.crpt.ru/api/v3";
    private final String CONSTRUCT = "https://ismp.crpt.ru/api/v3";

    private final RequestLimit requestLimit;

	public CrptApi(TimeUnit timeUnit, int requestLimit) {
		if (requestLimit <= 0) {
			throw new IllegalArgumentException("requestLimit must be >0");
		}
		this.requestLimit = new RequestLimit(timeUnit, requestLimit);
	}
	public String convert(Object header) {
		String file = null;
		ObjectMapper mapper = new ObjectMapper();
		try {
			file = mapper.writeValueAsString(header);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		if(file != null)
			return file;
		else
			return "";
	}
	private String postRequest(String url, String body, ContentType type) {

		Content result = null;
		try {
			if (requestLimit.getRequestLimit() >= 0) {
				result = Request.Post(url).bodyString(body, type).execute().returnContent();
			} else {
				System.out.println("limit is full, please try later");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(result != null)
			return result.asString();
		else
			return "";
	}

	public String createRequest(Document document, String signature)
	{
		String createJson = Base64.getEncoder().encode(convert(document).getBytes()).toString();
		DocumentHeader header = new DocumentHeader( Code.LP_INTRODUCE_GOODS,  signature , FormatForDocument.MANUAL, createJson);
		String bodyForJson = convert(header);
		return postRequest(URL.concat(CONSTRUCT) , bodyForJson , ContentType.APPLICATION_JSON);
	}
    @Data
    public class RequestLimit
	{
        private final TimeUnit timeUnit;
        private AtomicInteger limit;
        private AtomicBoolean checkingTime = new AtomicBoolean(true);

		private final int limitInt;
        public RequestLimit(TimeUnit timeUnit, int limit) {
            this.timeUnit = timeUnit;
            this.limit = new AtomicInteger(limit);
			limitInt = limit;
        }

        public void runRequestLimit()
		{
            try {
                Thread.sleep(timeUnit.toMillis(10));
                checkingTime.set(true);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

		public int getRequestLimit()
		{
			if(checkingTime.get() == true)
			{
				checkingTime.set(false);
				limit.set(limitInt);
				new Thread(this::runRequestLimit).start();
			}
			return limit.decrementAndGet();
		}
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public class Product
	{
		String certificateDocument;
		String certificateDocumentDate;
		String certificateDocumentNumber;
		String ownerInn;
		String producerInn;
		String productionDate;
		String tnvedCode;
		String uitCode;
		String uituCode;
    }
	@Data
	@AllArgsConstructor
	public class Document
	{

		private String description;
		private String docId;
		private String docStatus;
		private String docType;
		private boolean importRequest;
		private String participantInn;
		private String producerInn;
		private String productionDate;
		private String productionType;
		private Product products;  // не совсем понял, должны ли идти продукты как массив или как единичный предмет
		private String regFate;
		private String regNumber;
	}

	public enum FormatForDocument
	{
		XML , MANUAL
	}
	@Data
	@AllArgsConstructor
	public  class DocumentHeader{
		private Code code;
		private String signature;
		private FormatForDocument format;
		private String productDocument;
	}
	public enum Code
	{
		LP_INTRODUCE_GOODS;
	}
}
