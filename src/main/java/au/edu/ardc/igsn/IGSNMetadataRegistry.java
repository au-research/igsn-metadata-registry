package au.edu.ardc.igsn;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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

@OpenAPIDefinition(
        info = @Info(
                title = "${app.name}",
                description = "${app.description}",
                contact = @Contact(
                        name = "${app.contact.name}",
                        email = "${app.contact.email}",
                        url = "${app.contact.url}"
                ),
                version = "${app.version}",
                license = @License(
                        name = "Apache 2.0",
                        url = "https://www.apache.org/licenses/LICENSE-2.0.html"
                )
        )
)
public class IGSNMetadataRegistry {

    public static void main(String[] args) {
        SpringApplication.run(IGSNMetadataRegistry.class, args);
    }

}
