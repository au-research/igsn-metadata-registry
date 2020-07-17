package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.service.KeycloakService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.authorization.Permission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "About Me", description = "Display information about the current logged in user")
@SecurityRequirement(name = "basic")
@SecurityRequirement(name = "oauth2")
public class MeController {

    @Autowired
    KeycloakService kcService;

    @GetMapping(value = "/api/me", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Describes the current logged in user",
            description = "Retrieve the current user details"
    )
    @ApiResponse(responseCode = "200")
    public ResponseEntity<?> whoami(HttpServletRequest request) {
        // todo rather than returns a Map, returns a POJO for Schema API documentation (eg. UserDetail)
        Map<String, Object> map = new LinkedHashMap<>();

        AccessToken token = kcService.getAccessToken(request);

        map.put("id", token.getSubject());
        map.put("username", token.getPreferredUsername());
        map.put("name", token.getName());
        map.put("email", token.getEmail());
        map.put("roles", token.getRealmAccess().getRoles());

        //map.put("otherClaims", token.getOtherClaims());
        Map<String, Object> otherClaims = token.getOtherClaims();
        if (otherClaims.containsKey("groups")) {
            map.put("groups", otherClaims.get("groups"));
        }
        // map.put("resource access", token.getResourceAccess());
//         map.put("scope", token.getScope());

        // attempt to get available resources (if any)
        String accessToken = kcService.getPlainAccessToken(request);
        ArrayList<Object> allocations = new ArrayList<>();

        List<Permission> resources = kcService.getAuthorizedResources(accessToken);
        for (Permission permission: resources) {
            Map<String, Object> resourceMap = new LinkedHashMap<>();
            resourceMap.put("id", permission.getResourceId());
            resourceMap.put("name", permission.getResourceName());
            resourceMap.put("scopes", permission.getScopes());
            allocations.add(resourceMap);
        }

        map.put("allocations", allocations);

        return ResponseEntity.ok().body(map);
    }
}
