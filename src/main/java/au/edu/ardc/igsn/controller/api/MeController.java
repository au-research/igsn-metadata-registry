package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.User;
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
    public ResponseEntity<User> whoami(HttpServletRequest request) {

        User user = kcService.getLoggedInUser(request);
        return ResponseEntity.ok(user);
    }
}
