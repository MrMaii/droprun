package app.droprun;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Strict, JVM-testable destination parsing. A code alone never selects a server. */
final class PairingTarget {
    final String relay, instanceId, code;
    PairingTarget(String relay, String instanceId, String code) {
        URI origin = URI.create(relay.trim());
        if (!"https".equalsIgnoreCase(origin.getScheme()) || origin.getHost() == null || origin.getUserInfo() != null
                || origin.getQuery() != null || origin.getFragment() != null
                || !(origin.getPath().isEmpty() || "/".equals(origin.getPath()))
                || (origin.getPort() != -1 && origin.getPort() != 443)) throw new IllegalArgumentException("Use an HTTPS Relay origin without a path.");
        this.relay = "https://" + origin.getHost().toLowerCase(Locale.ROOT);
        if(!instanceId.trim().matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))throw new IllegalArgumentException("Use the instance ID from your computer.");
        this.instanceId = UUID.fromString(instanceId.trim()).toString();
        this.code = code.toUpperCase(Locale.ROOT).replaceAll("[\\s-]", "");
        if (!this.code.matches("[0-9A-F]{20}")) throw new IllegalArgumentException("The pairing code must contain 20 hexadecimal characters.");
    }
    static PairingTarget parse(String value) {
        try {
            URI uri = URI.create(value.trim());
            if (!"droprun".equals(uri.getScheme()) || !"pair".equals(uri.getHost()) || uri.getFragment() != null
                    || uri.getUserInfo() != null || !(uri.getPath().isEmpty() || "/".equals(uri.getPath()))) return null;
            Map<String, String> values = new HashMap<>();
            for (String pair : uri.getRawQuery().split("&")) {
                String[] parts = pair.split("=", 2);
                if (parts.length != 2 || values.put(parts[0], URLDecoder.decode(parts[1], "UTF-8")) != null) return null;
            }
            return new PairingTarget(values.get("relay"), values.get("instance"), values.get("code"));
        } catch (Exception invalid) { return null; }
    }
}
