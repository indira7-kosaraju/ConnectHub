package com.connecthub.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityScheme;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.PropertyCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI connectHubOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ConnectHub API")
                        .version("v1")
                        .description("Circles, vibes and moments. All responses use the { success, data, error, timestamp } envelope."))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    /**
     * Response DTOs always serialise every property (nulls included), so mark them all required.
     * Genuinely nullable fields carry {@code @Schema(nullable = true)}. This makes the generated
     * TypeScript types exact: {@code string} vs {@code string | null}, never {@code string | undefined}.
     */
    @Bean
    public OpenApiCustomizer responsePropertiesRequired() {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            openApi.getComponents().getSchemas().forEach((name, schema) -> {
                if (name.endsWith("Request")) {
                    return;
                }
                @SuppressWarnings("unchecked")
                Schema<Object> s = (Schema<Object>) schema;
                // Undo swagger-core hoisting a field-level @Schema(nullable) onto the referenced component
                // (see nullableRefs below, which puts it on the property instead).
                if (Boolean.TRUE.equals(s.getNullable()) && "object".equals(s.getType())) {
                    s.setNullable(null);
                    s.setDescription(null);
                }
                if (s.getProperties() != null && !s.getProperties().isEmpty()) {
                    var required = new ArrayList<>(s.getProperties().keySet());
                    if (name.equals("ApiError")) {
                        required.remove("fields"); // omitted unless there are field errors
                    }
                    s.setRequired(required);
                }
            });
        };
    }

    /**
     * OpenAPI 3.0 can't mark a bare {@code $ref} nullable, so a {@code @Schema(nullable = true)} field
     * whose type is another DTO becomes {@code allOf: [$ref], nullable: true} — which openapi-typescript
     * renders as {@code X | null} without making X itself nullable everywhere.
     */
    @Bean
    public PropertyCustomizer nullableRefs() {
        return (property, type) -> {
            if (property == null || property.get$ref() == null || type.getCtxAnnotations() == null) {
                return property;
            }
            for (Annotation a : type.getCtxAnnotations()) {
                if (a instanceof io.swagger.v3.oas.annotations.media.Schema ann && ann.nullable()) {
                    Schema<Object> wrapped = new Schema<>();
                    wrapped.setNullable(true);
                    wrapped.setAllOf(List.of(new Schema<>().$ref(property.get$ref())));
                    if (!ann.description().isEmpty()) {
                        wrapped.setDescription(ann.description());
                    }
                    return wrapped;
                }
            }
            return property;
        };
    }
}
