package au.edu.ardc.igsn.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * A service that allows interaction with the Keycloak server manually
 */
@Service
public class KeycloakService {

    @Value("${keycloak.auth-server-url:https://test.auth.ardc.edu.au/auth/}")
    private String kcAuthServerURL;

    @Value("${keycloak.realm:ARDC}")
    private String kcRealm;

    @Value("${keycloak.resource:igsn}")
    private String clientID;

    @Value("${keycloak.credentials.secret:secret}")
    private String clientSecret;

    /**
     * Get the Keycloak Access Token for the current request
     *
     * @param request the current HttpServletRequest
     * @return AccessToken access token
     */
    public AccessToken getAccessToken(HttpServletRequest request) {
        KeycloakSecurityContext keycloakSecurityContext = (KeycloakSecurityContext) (request.getAttribute(KeycloakSecurityContext.class.getName()));

        return keycloakSecurityContext.getToken();
    }

    /**
     * Returns the OAuth2 Access Token as String for the current request
     *
     * @param request the current HttpServletRequest
     * @return String access token
     */
    public String getPlainAccessToken(HttpServletRequest request) {
        KeycloakSecurityContext keycloakSecurityContext = (KeycloakSecurityContext) (request.getAttribute(KeycloakSecurityContext.class.getName()));

        return keycloakSecurityContext.getTokenString();
    }

    /**
     * Returns a list of protected resources that are available to the current user
     *
     * @param accessToken the access token as string
     * @return a list of resources
     * @throws IOException exception
     */
    public List<JsonNode> getAuthorizedResources(String accessToken) throws IOException {
        OkHttpClient client = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .add("grant_type", "urn:ietf:params:oauth:grant-type:uma-ticket")
                .add("audience", clientID)
                .add("client_secret", clientSecret)
                .add("response_mode", "permissions")
                .build();

        Request authzReq = new Request.Builder()
                .url(kcAuthServerURL + "/realms/" + kcRealm + "/protocol/openid-connect/token")
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(formBody)
                .build();

        Response response = client.newCall(authzReq).execute();
        String responseBody = Objects.requireNonNull(response.body()).string();

        // TODO map available resources to Allocations instead
        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.readValue(
                responseBody,
                new TypeReference<List<JsonNode>>() {
                });
    }

}
