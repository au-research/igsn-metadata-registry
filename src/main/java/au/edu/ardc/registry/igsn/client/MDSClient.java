package au.edu.ardc.registry.igsn.client;

import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

import au.edu.ardc.registry.exception.MDSClientConfigurationException;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

public class MDSClient {

	Logger logger = LoggerFactory.getLogger(MDSClient.class);

	@Value("${mds.url}")
	private String mds_url;

	private WebClient web_client;

	/*
	 *
	 * WebClient with Basic Authentication
	 *
	 */
	public MDSClient(IGSNAllocation allocation) throws MDSClientConfigurationException {
		String mds_username = allocation.getMds_username();
		String mds_password = allocation.getMds_password();
		String mds_url = allocation.getMds_url();
		if (mds_username == null)
			throw new MDSClientConfigurationException("MDS user name is not configured");
		if (mds_password == null)
			throw new MDSClientConfigurationException("MDS password is not configured");
		if (mds_url == null)
			throw new MDSClientConfigurationException("MDS url is not configured");
		this.web_client = WebClient.builder().filter(basicAuthentication(mds_username, mds_password)).baseUrl(mds_url)
				.build();
		this.mds_url = mds_url;

	}

	public String getUrl() {
		return this.mds_url;
	}

	public int mintIGSN(String registrationMetadata, String identifier, String landingPage, boolean testMode)
			throws Exception {
		int response_code;
		try {
			response_code = createIdentifier(identifier, landingPage, testMode);
			if (!(testMode))
				response_code = addMetadata(registrationMetadata);
		}
		catch (Exception e) {
			throw e;
		}
		return response_code;
	}

	private int createIdentifier(String identifier, String landing_page, boolean testMode) throws Exception {
		String test_param = "?testMode=true";
		String service_url = "igsn";

		int response_code = 0;
		if (testMode)
			service_url += test_param;

		String mint_content = "igsn=" + identifier;
		mint_content += "\n";
		mint_content += "url=" + landing_page;

		try {
			ClientResponse response = this.web_client.post().uri(service_url).contentType(MediaType.TEXT_PLAIN)
					.body(BodyInserters.fromPublisher(Mono.just(mint_content), String.class)).exchange().block();
			response_code = response.statusCode().value();
			if (response_code != 201) {
				throw new Exception(response.bodyToMono(String.class).block());
			}
		}
		catch (Exception e) {
			throw e;
		}
		return response_code;
	}

	private int addMetadata(String registrationMetadata) throws Exception {

		String service_url = "metadata";
		System.out.println(" registrationMetadata:" + registrationMetadata);
		int response_code = 0;

		try {
			ClientResponse response = this.web_client.post().uri(service_url).contentType(MediaType.APPLICATION_XML)
					.body(BodyInserters.fromPublisher(Mono.just(registrationMetadata), String.class)).exchange()
					.block();
			response_code = response.rawStatusCode();
			if (response_code != 201) {
				throw new Exception(response.bodyToMono(String.class).block());
			}
		}
		catch (Exception e) {
			throw e;
		}

		return response_code;
	}

	public String getIGSNMetadata(String identifier) throws Exception {

		String service_url = "metadata/" + identifier;
		String registrationMetadata = "";

		try {
			ClientResponse response = this.web_client.get().uri(service_url).exchange().block();
			int response_code = response.rawStatusCode();
			if (response_code == 200) {
				registrationMetadata = response.bodyToMono(String.class).block();
			}
			else {
				throw new Exception(response.bodyToMono(String.class).block());
			}
		}
		catch (Exception e) {
			throw e;
		}
		return registrationMetadata;
	}

	public String getIGSNLandingPage(String identifier) throws Exception {

		String service_url = "igsn/" + identifier;
		String url = "";

		try {
			ClientResponse response = this.web_client.get().uri(service_url).exchange().block();
			int response_code = response.rawStatusCode();
			if (response_code == 200) {
				url = response.bodyToMono(String.class).block();
			}
			else {
				throw new Exception(response.bodyToMono(String.class).block());
			}
		}
		catch (Exception e) {
			throw e;
		}
		return url;
	}

}
