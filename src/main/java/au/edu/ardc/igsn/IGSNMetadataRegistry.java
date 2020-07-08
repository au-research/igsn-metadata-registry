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
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

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

    @Value("${app.url:/}")
    String appURL;

    @Value("${app.name:Current Server}")
    String appDescription;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().addServersItem(new Server().url(appURL).description(appDescription));
    }

    public static void main(String[] args) {
        SpringApplication.run(IGSNMetadataRegistry.class, args);
    }

}
