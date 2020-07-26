package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.model.Allocation;
import au.edu.ardc.igsn.model.Scope;
import au.edu.ardc.igsn.model.User;
import au.edu.ardc.igsn.model.DataCenter;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.representation.TokenIntrospectionResponse;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
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

    @Autowired
    private Environment env;

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
    }

    public User getLoggedInUser(HttpServletRequest request) {
        AccessToken token = getAccessToken(request);

        User user = new User(UUID.fromString(token.getSubject()));
        user.setUsername(token.getPreferredUsername());
        user.setName(token.getName());
        user.setEmail(token.getEmail());
        user.setRoles(new ArrayList<>(token.getRealmAccess().getRoles()));

        // groups belongs to otherClaims
        Map<String, Object> otherClaims = token.getOtherClaims();
        if (otherClaims.containsKey("groups")) {
            List<String> groups = new ArrayList<>();
            groups.addAll((Collection<? extends String>) otherClaims.get("groups"));
            List<DataCenter> userDataCenters = new ArrayList<>();
            for (String group : groups) {
                try {
                    DataCenter dc = getDataCenterByGroupName(group);
                    userDataCenters.add(dc);
                } catch (Exception e) {
                    // log exception here, user doesn't get this datacenter
                }
            }
            user.setDataCenters(userDataCenters);
            // user.setGroups(groups);
        }

        // Allocations in authorizedResources
        List<Permission> permissions = getAuthorizedResources(getPlainAccessToken(request));
        List<Allocation> userPermissions = new ArrayList<>();
        for (Permission permission : permissions) {
            try {
                Allocation allocation = getAllocationByResourceID(permission.getResourceId());
                List<Scope> scopes = new ArrayList<>();
                for (String scope : permission.getScopes()) {
                    scopes.add(Scope.fromString(scope));
                }
                allocation.setScopes(scopes);
                userPermissions.add(allocation);
            } catch (Exception ex) {
                // todo logging
                System.out.println(ex);
            }
        }
        user.setPermissions(userPermissions);

        user.setAllocations(getAuthorizedResources(getPlainAccessToken(request)));
        return user;
    }

    /**
     * @param name The name (path) of the Group
     * @throws Exception normally when the environment is not properly set or (rarely) group name doesn't exist
     * @return DataCenter representation of a keycloak Group
     */
    public DataCenter getDataCenterByGroupName(String name) throws Exception {
        Keycloak keycloak = getAdminClient();
        GroupRepresentation group = keycloak.realm(kcRealm).getGroupByPath(name);

        DataCenter dataCenter = new DataCenter(UUID.fromString(group.getId()));
        dataCenter.setName(group.getName());
        return dataCenter;
    }

    public Allocation getAllocationByResourceID(String id) throws Exception {
        AuthzClient authzClient = getAuthzClient();
        ResourceRepresentation resource = authzClient.protection().resource().findById(id);

        Allocation allocation = new Allocation(UUID.fromString(resource.getId()));
        allocation.setName(resource.getName());
        allocation.setType(resource.getType());
        Map<String, List<String>> attributes = resource.getAttributes();
        allocation.setStatus(attributes.containsKey("status") ? String.valueOf(attributes.get("status")): null);
        return allocation;
    }

    /**
     * @return an Admin Keycloak client
     * @throws Exception when the environment is not properly set
     */
    private Keycloak getAdminClient() throws Exception {
        String username = env.getProperty("keycloak-admin.username");
        String password = env.getProperty("keycloak-admin.password");
        if (username == null || password == null) {
            throw new Exception("Keycloak credentials is not properly configured");
        }
        Keycloak keycloak = KeycloakBuilder
                .builder().serverUrl(kcAuthServerURL)
                .realm("master").username(username).password(password)
                .clientId("admin-cli")
                .resteasyClient(new ResteasyClientBuilder().connectionPoolSize(10).build())
                .build();

        return keycloak;
    }

    private AuthzClient getAuthzClient() throws Exception {
        Configuration configuration = new Configuration();
        configuration.setAuthServerUrl(kcAuthServerURL);
        configuration.setRealm(kcRealm);
        configuration.setResource(clientID);
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("secret", clientSecret);
        configuration.setCredentials(credentials);

        AuthzClient authzClient = AuthzClient.create(configuration);

        return authzClient;
    }

}
