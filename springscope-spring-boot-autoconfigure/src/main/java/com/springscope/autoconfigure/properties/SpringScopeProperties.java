package com.springscope.autoconfigure.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "springscope")
public class SpringScopeProperties {

    /**
     * Whether SpringScope inspection services and REST endpoints are enabled.
     * Intended for local development. Disable in production unless the endpoints
     * are otherwise protected.
     */
    private boolean enabled = true;

    @Valid
    private final Api api = new Api();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Api getApi() {
        return api;
    }

    public static class Api {

        /**
         * Base path for the SpringScope REST API. Must start with a slash.
         */
        @NotBlank
        @Pattern(regexp = "^/.*", message = "API base path must start with '/'")
        private String basePath = "/springscope/api";

        public String getBasePath() {
            return basePath;
        }

        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }
    }
}
