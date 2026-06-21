package com.mykare.appointment_service.Config;

import com.mykare.appointment_service.Common.Constants.HeaderConstants;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    public static final String JWT_SECURITY_SCHEME =
            "Bearer Authentication";

    @Bean
    public OpenAPI appointmentServiceOpenApi() {

        SecurityScheme jwtSecurityScheme =
                new SecurityScheme()
                        .name(JWT_SECURITY_SCHEME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description(
                                "Enter the JWT access token received from the login API. " +
                                        "Do not include the word Bearer."
                        );

        return new OpenAPI()
                .info(
                        new Info()
                                .title("Healthcare Appointment Service API")
                                .description(
                                        """
                                        Backend APIs for the healthcare appointment platform.

                                        Features:
                                        - User registration and login
                                        - JWT authentication and logout
                                        - Appointment slot management
                                        - Appointment booking and cancellation
                                        - Kafka-based notification processing
                                        - Appointment history tracking

                                        Every API request must contain the X-Transaction-Id header.
                                        """
                                )
                                .version("1.0.0")
                                .contact(
                                        new Contact()
                                                .name("MyKare Development Team")
                                )
                                .license(
                                        new License()
                                                .name("Private")
                                )
                )
                .servers(
                        List.of(
                                new Server()
                                        .url("http://localhost:8080")
                                        .description(
                                                "Local and Docker environment"
                                        )
                        )
                )
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        JWT_SECURITY_SCHEME,
                                        jwtSecurityScheme
                                )
                )
                .externalDocs(
                        new ExternalDocumentation()
                                .description(
                                        "Healthcare Appointment Platform"
                                )
                );
    }

    /**
     * Adds X-Transaction-Id as a mandatory header
     * to every documented API operation.
     */
    @Bean
    public OpenApiCustomizer transactionIdHeaderCustomizer() {

        return openApi -> {

            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths()
                    .values()
                    .forEach(pathItem ->
                            pathItem.readOperations()
                                    .forEach(operation -> {

                                        boolean alreadyPresent =
                                                operation.getParameters() != null
                                                        && operation
                                                        .getParameters()
                                                        .stream()
                                                        .anyMatch(parameter ->
                                                                HeaderConstants
                                                                        .TRANSACTION_ID
                                                                        .equalsIgnoreCase(
                                                                                parameter.getName()
                                                                        )
                                                        );

                                        if (!alreadyPresent) {
                                            operation.addParametersItem(
                                                    new Parameter()
                                                            .name(
                                                                    HeaderConstants
                                                                            .TRANSACTION_ID
                                                            )
                                                            .in("header")
                                                            .required(true)
                                                            .description(
                                                                    "Unique transaction ID supplied by the client for end-to-end request tracing"
                                                            )
                                                            .example(
                                                                    "TXN-BOOKING-10001"
                                                            )
                                                            .schema(
                                                                    new StringSchema()
                                                                            .minLength(1)
                                                            )
                                            );
                                        }
                                    })
                    );
        };
    }
}