package au.edu.ardc.igsn;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;

@SpringBootApplication
@SecuritySchemes({
        @SecurityScheme(
                name = "basic",
                description = "Basic Authentication",
                type = SecuritySchemeType.HTTP,
                scheme = "basic"
        ),
        @SecurityScheme(
                name = "oauth2",
                type = SecuritySchemeType.OAUTH2,
                in = SecuritySchemeIn.HEADER,
                flows = @OAuthFlows(
                        password = @OAuthFlow(
                                tokenUrl = "${keycloak-token-url}"
                        )
                )
        )
})
public class IGSNMetadataRegistry {

    public static void main(String[] args) {
        SpringApplication.run(IGSNMetadataRegistry.class, args);
    }

}
