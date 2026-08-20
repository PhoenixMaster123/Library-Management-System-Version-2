package app.infrastructure.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configures the OpenAPI documentation. */
@Configuration
public class OpenApiConfig {

    static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI libraryOpenApi() {
        return new OpenAPI()
                .info(apiInfo())
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, bearerScheme()))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    private static Info apiInfo() {
        return new Info()
                .title("Library Management System API")
                .version("v1")
                .description("""
                        Catalogue, members and loans.

                        Sign in through POST /api/login, then paste the returned token into
                        Authorize. Endpoints under /admin, /customers and /transactions/history
                        require an administrator.""")
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT"));
    }

    private static SecurityScheme bearerScheme() {
        return new SecurityScheme()
                .name(BEARER_SCHEME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("The token from POST /api/login, without the \"Bearer \" prefix.");
    }
}
