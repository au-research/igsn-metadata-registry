package au.edu.ardc.registry;

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
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching
@EnableAsync
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
                license = @License(
                        name = "Apache 2.0",
                        url = "https://www.apache.org/licenses/LICENSE-2.0.html"
                )
        )
)
public class MetadataRegistry {

    @Value("${app.url:/}")
    String appURL;

    @Value("${app.name:Current Server}")
    String appDescription;

    public static void main(String[] args) {
        SpringApplication.run(MetadataRegistry.class, args);
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().addServersItem(new Server().url(appURL).description(appDescription));
    }

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

}
