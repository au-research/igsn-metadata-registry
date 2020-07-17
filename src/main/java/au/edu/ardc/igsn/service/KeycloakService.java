package au.edu.ardc.igsn.service;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.representation.TokenIntrospectionResponse;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.Permission;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

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
     * Get the current logged in user UUID
     *
     * @param request current HttpServletRequest
     * @return UUID of the current logged in user
     */
    public UUID getUserUUID(HttpServletRequest request) {
        AccessToken kcToken = getAccessToken(request);
        String subject = kcToken.getSubject();

        return UUID.fromString(subject);
    }

    /**
     * Returns a list of protected resources that are available to the current user
     *
     * @param accessToken the access token as string
     * @return a list of resources
     */
    public List<Permission> getAuthorizedResources(String accessToken) {

        // to use the authzClient to obtain permission require a keycloak.json file
        // that duplicate the information that is already available in application.properties
        // the Bean KeycloakConfigResolver does not work with AuthzClient yet

        // build a configuration out of the properties
        Configuration configuration = new Configuration();
        configuration.setAuthServerUrl(kcAuthServerURL);
        configuration.setRealm(kcRealm);
        configuration.setResource(clientID);
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("secret", clientSecret);
        configuration.setCredentials(credentials);

        // use the authzClient with that configuration so it doesn't fall back to keycloak.json
        try {
            AuthzClient authzClient = AuthzClient.create(configuration);
            AuthorizationResponse authzResponse = authzClient.authorization(accessToken).authorize();
            String rpt = authzResponse.getToken();
            TokenIntrospectionResponse requestingPartyToken = authzClient.protection().introspectRequestingPartyToken(rpt);
            return requestingPartyToken.getPermissions();
        } catch (Exception e) {
            // return a blank list if there's an authorization error
            return new ArrayList<>();
        }

        // the rpt will contain all the permissions

    }

}
