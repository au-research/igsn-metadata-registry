package au.edu.ardc.registry.igsn.client;

import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

import au.edu.ardc.registry.exception.MDSClientConfigurationException;
import au.edu.ardc.registry.igsn.model.IGSNAllocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

/**
 * https://doidb.wdc-terra.org/igsn/static/apidoc a client implementation for the IGSN
 * (MDS) API service
 *
 */
public class MDSClient {

	Logger logger = LoggerFactory.getLogger(MDSClient.class);

	private WebClient web_client;

	/**
	 * @param allocation and IGSN allocation that must contain username , password and
	 * server_url
	 * @throws MDSClientConfigurationException if any of the values are not set in the
	 * allocation
	 */
	public MDSClient(IGSNAllocation allocation) throws MDSClientConfigurationException {
		String mds_username = allocation.getMds_username();
		String mds_password = allocation.getMds_password();
		String mds_url = allocation.getMds_url();
		if (mds_username == null)
			throw new MDSClientConfigurationException(MDSClientConfigurationException.Configuration.user_name);
		if (mds_password == null)
			throw new MDSClientConfigurationException(MDSClientConfigurationException.Configuration.password);
		if (mds_url == null)
			throw new MDSClientConfigurationException(MDSClientConfigurationException.Configuration.server_url);
		this.web_client = WebClient.builder().filter(basicAuthentication(mds_username, mds_password)).baseUrl(mds_url)
				.build();
	}

	/**
	 * @param registrationMetadata XML String of the registration metadata
	 * @param identifier the IGSN Identifier
	 * @param landingPage the URL of the landing page of the IGSN
	 * @param testMode if test mode is true the handle won't be minted
	 * @return the response code (201 CREATED)
	 * @throws Exception If response code is not 201 containing the response body if
	 * present
	 */
	public int mintIGSN(String registrationMetadata, String identifier, String landingPage, boolean testMode)
			throws Exception {
		int response_code;
		try {
			response_code = createIdentifier(identifier, landingPage, testMode);
			// if testmode is true the handle is not created hence no metadata can be
			// added to it
			if (!(testMode))
				response_code = addMetadata(registrationMetadata);
		}
		catch (HttpServerErrorException e) {
			throw e;
		}
		return response_code;
	}

	/**
	 *
	 * DELETE URI: https://doidb.wdc-terra.org/igsn/metadata/{igsn} where {igsn} is a
	 * specific IGSN. This request marks a dataset as 'inactive'. To activate it again,
	 * POST new metadata or set the isActive-flag in the user interface.
	 * @param identifier the IGSN identifier as String
	 * @return the response code of the DELETE request
	 * @throws Exception If response code is not 200 containing the response body if
	 * present
	 */
	public int deactivateIGSN(String identifier) throws Exception {
		String service_url = "metadata/" + identifier;
		try {
			ClientResponse response = this.web_client.delete().uri(service_url).exchange().block();
			if (response != null && response.rawStatusCode() == 200) {
				return response.rawStatusCode();
			}
			else if (response != null) {
				throw new Exception(response.bodyToMono(String.class).block());
			}
		}
		catch (Exception e) {
			throw e;
		}
		return 0;
	}

	/**
	 * Create an IGSN Identifier (handle)
	 * @param identifier the IGSN Identifier
	 * @param landingPage the URL of the landing page of the IGSN
	 * @param testMode if test mode is true the handle won't be minted
	 * @return the response code (201 CREATED)
	 * @throws Exception if response code is not 201
	 */
	private int createIdentifier(String identifier, String landingPage, boolean testMode) throws Exception {
		String test_param = "?testMode=true";
		String service_url = "igsn";

		int response_code = 0;
		if (testMode)
			service_url += test_param;

		String mint_content = "igsn=" + identifier;
		mint_content += "\n";
		mint_content += "url=" + landingPage;

		ClientResponse response = this.web_client.post().uri(service_url).contentType(MediaType.TEXT_PLAIN)
				.body(BodyInserters.fromPublisher(Mono.just(mint_content), String.class)).exchange().block();
		if (response != null) {
			response_code = response.statusCode().value();
			if (response_code != 201) {
				throw new Exception(response.bodyToMono(String.class).block());
			}
		}
		return response_code;
	}

	/**
	 * Add Registration metadata the Identifier is contained in the metadata no other info
	 * eg:parameter is needed
	 * @param registrationMetadata XML String of the registration metadata
	 * @return the response code (201 CREATED)
	 * @throws Exception if response code is not 201
	 */
	private int addMetadata(String registrationMetadata) throws Exception {

		String service_url = "metadata";
		try {
			ClientResponse response = this.web_client.post().uri(service_url).contentType(MediaType.APPLICATION_XML)
					.body(BodyInserters.fromPublisher(Mono.just(registrationMetadata), String.class)).exchange()
					.block();
			if (response != null && response.rawStatusCode() == 201) {
				return response.rawStatusCode();
			}
			assert response != null;
			throw new Exception(response.bodyToMono(String.class).block());
		}
		catch (Exception e) {
			throw e;
		}
	}

	/**
	 * Get the latest registration metadata for a given IGSN record
	 * @param identifier the IGSN value
	 * @return the XML registration metadata as String
	 * @throws Exception if response code is not 200
	 */
	public String getIGSNMetadata(String identifier) throws Exception {

		String service_url = "metadata/" + identifier;
		return doGetRequest(service_url);
	}

	/**
	 * Get the URL of the landing page for a given IGSN record
	 * @param identifier the IGSN value
	 * @return URL of the landing page as String
	 * @throws Exception if response code is not 200
	 */
	public String getIGSNLandingPage(String identifier) throws Exception {

		String service_url = "igsn/" + identifier;
		return doGetRequest(service_url);
	}

	/**
	 * executes a GET request and returns the body content
	 * @param service_url the url of the request
	 * @return the body content as String
	 * @throws Exception if the response code is not 200
	 */
	@Nullable
	private String doGetRequest(String service_url) throws Exception {
		try {
			ClientResponse response = this.web_client.get().uri(service_url).exchange().block();
			if (response != null && response.rawStatusCode() == 200) {
				return response.bodyToMono(String.class).block();
			}
			else if (response != null) {
				throw new Exception(response.bodyToMono(String.class).block());
			}
		}
		catch (Exception e) {
			throw e;
		}
		return null;
	}

}
