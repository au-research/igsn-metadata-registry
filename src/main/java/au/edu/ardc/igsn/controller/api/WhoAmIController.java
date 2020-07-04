package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.service.KeycloakService;
import com.fasterxml.jackson.databind.JsonNode;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.authorization.Permission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class WhoAmIController {

    @Autowired
    KeycloakService kcService;

    @GetMapping("/api/whoami")
    public ResponseEntity<?> whoami(HttpServletRequest request) {
        Map<String, Object> map = new LinkedHashMap<>();

        AccessToken token = kcService.getAccessToken(request);

        map.put("id", token.getSubject());
        map.put("username", token.getPreferredUsername());
        map.put("name", token.getName());
        map.put("email", token.getEmail());
        map.put("roles", token.getRealmAccess().getRoles());
        map.put("otherClaims", token.getOtherClaims());
        // map.put("resource access", token.getResourceAccess());
        // map.put("scope", token.getScope());

        // attempt to get available resources (if any)
        String accessToken = kcService.getPlainAccessToken(request);
        List<Permission> resources = kcService.getAuthorizedResources(accessToken);
        map.put("resources", resources);

        return ResponseEntity.ok().body(map);
    }
}
