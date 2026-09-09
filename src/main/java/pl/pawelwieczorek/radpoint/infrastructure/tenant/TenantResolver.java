package pl.pawelwieczorek.radpoint.infrastructure.tenant;

import com.sun.net.httpserver.Headers;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class TenantResolver {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final Pattern TENANT_PATTERN =
            Pattern.compile("[a-z0-9]{1,32}");

    public String resolve(Headers headers) {
        String tenantFromHeader = headers.getFirst(TENANT_HEADER);

        if (tenantFromHeader != null && !tenantFromHeader.isBlank()) {
            return validate(tenantFromHeader);
        }

        String host = headers.getFirst("Host");
        String tenantFromHost = extractTenantFromHost(host)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tenant is missing. Provide X-Tenant-ID or a valid Host header."
                ));

        return validate(tenantFromHost);
    }

    private Optional<String> extractTenantFromHost(String host) {
        if (host == null || host.isBlank()) {
            return Optional.empty();
        }

        String hostWithoutPort = host.trim()
                .toLowerCase(Locale.ROOT)
                .replaceFirst(":\\d+$", "");

        String[] parts = hostWithoutPort.split("\\.");

        if (parts.length < 4) {
            return Optional.empty();
        }

        String environment = parts[0];
        String tenant = parts[1];

        if (environment.isBlank() || tenant.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(tenant);
    }

    private String validate(String value) {
        value = value.trim();

        if (!TENANT_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Invalid tenant ID. Use 1-32 lowercase letters or digits."
            );
        }

        return value;
    }
}
