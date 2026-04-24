package pkg.vms.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Loads redemption page HTML from resources.
 */
public final class RedemptionPageLoader {

    private static final String PAGE_RESOURCE = "pkg/vms/redemption/index.html";
    private static final String API_BASE_TOKEN = "__API_BASE_URL__";

    private RedemptionPageLoader() {
    }

    public static String loadPageHtml(String apiBaseUrl) {
        try (InputStream in = RedemptionPageLoader.class.getClassLoader().getResourceAsStream(PAGE_RESOURCE)) {
            if (in == null) {
                return "<html><body><h2>Redemption page not found.</h2></body></html>";
            }
            String html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            String safeApiBase = apiBaseUrl == null ? "" : apiBaseUrl;
            return html.replace(API_BASE_TOKEN, safeApiBase);
        } catch (IOException e) {
            return "<html><body><h2>Failed to load redemption page.</h2></body></html>";
        }
    }
}
