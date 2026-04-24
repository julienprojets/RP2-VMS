package pkg.vms.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Centralized settings for redemption UI/API endpoints.
 */
public final class RedemptionConfig {

    private static final String CONFIG_FILE = "redemption_config.properties";
    private static final Properties PROPS = new Properties();
    private static volatile boolean loaded = false;

    private RedemptionConfig() {
    }

    private static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }

        // Load defaults from classpath (optional)
        try (InputStream in = RedemptionConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in != null) {
                PROPS.load(in);
            }
        } catch (IOException ignored) {
            // Use built-in defaults if classpath file cannot be loaded
        }

        // Allow override from working directory
        Path externalPath = Path.of(System.getProperty("user.dir"), CONFIG_FILE);
        if (Files.exists(externalPath)) {
            try (InputStream in = Files.newInputStream(externalPath)) {
                Properties externalProps = new Properties();
                externalProps.load(in);
                PROPS.putAll(externalProps);
            } catch (IOException ignored) {
                // Keep classpath/default values
            }
        }

        loaded = true;
    }

    public static String getHostedPageBaseUrl() {
        ensureLoaded();
        return trimToNull(PROPS.getProperty("redemption.hosted.page.base.url"));
    }

    public static String getApiBaseUrl() {
        ensureLoaded();
        return trimToNull(PROPS.getProperty("redemption.api.base.url"));
    }

    public static int getLocalServerPort() {
        ensureLoaded();
        String value = trimToNull(PROPS.getProperty("redemption.local.server.port"));
        if (value == null) {
            return 8080;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 8080;
        }
    }

    public static boolean isLocalServerEnabled() {
        ensureLoaded();
        String value = trimToNull(PROPS.getProperty("redemption.local.server.enabled"));
        if (value == null) {
            return true;
        }
        return Boolean.parseBoolean(value);
    }

    public static String buildVoucherAccessUrl(String voucherCode, String localBaseUrl) {
        String hostedBase = getHostedPageBaseUrl();
        if (hostedBase != null) {
            return hostedBase + "?code=" + voucherCode;
        }
        return localBaseUrl + "?code=" + voucherCode;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
