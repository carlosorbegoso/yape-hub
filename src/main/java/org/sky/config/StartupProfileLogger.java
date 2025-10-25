package org.sky.config;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

@ApplicationScoped
public class StartupProfileLogger {

    private static final Logger log = Logger.getLogger(StartupProfileLogger.class);

    void onStart(@Observes StartupEvent ev) {
        try {
            Config config = ConfigProvider.getConfig();

            // Prefer a Quarkus-provided property if available; fall back to <not-set>
            String activeProfile = config.getOptionalValue("quarkus.profile", String.class).orElse("<not-set>");
            String launchMode = System.getProperty("quarkus.launch-mode", System.getProperty("quarkus.launchMode", "<not-set>"));

            log.infof("Quarkus active profile (from config): %s", activeProfile);
            log.infof("Quarkus launch mode (from system properties if present): %s", launchMode);

            String quarkusProfileProp = config.getOptionalValue("quarkus.profile", String.class).orElse("<not-set>");
            String configLocations = config.getOptionalValue("quarkus.config.locations", String.class).orElse("<not-set>");
            String customProfile = config.getOptionalValue("spring.profiles.active", String.class).orElse("<not-set>");

            log.infof("quarkus.profile property: %s", quarkusProfileProp);
            log.infof("quarkus.config.locations: %s", configLocations);
            log.infof("spring.profiles.active (if present): %s", customProfile);
        } catch (Exception e) {
            log.error("Error while logging startup profile", e);
        }
    }
}
