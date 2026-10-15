package in.fixna.platform.websiteaudit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;

import org.springframework.stereotype.Component;

/**
 * Fetches a web resource for website audits with SSRF and DNS-rebinding
 * defenses (ADR-010).
 *
 * Policy: http/https only, ports 80/443 only. DNS is resolved exactly once
 * via {@link HostResolver}; every returned address is validated and the
 * connection is made to the pinned address with the original Host header
 * and TLS SNI plus HTTPS endpoint identification against the original
 * hostname. Fail-closed: a single blocked address rejects the host. No
 * redirect following, 512 KB body cap, hard connect/read timeouts,
 * HTTP/1.1 with Connection: close.
 */
@Component
public class WebsiteFetcher {

    static final int MAX_BODY_BYTES = 512 * 1024;
    static final int CONNECT_TIMEOUT_MS = 5_000;
    static final int REQUEST_TIMEOUT_MS = 10_000;
    static final String USER_AGENT = "FixnaWebsiteAudit/1.0";

    private static final Set<Integer> ALLOWED_PORTS = Set.of(-1, 80, 443);
    private static final Set<String> BLOCKED_HOSTS =
            Set.of("metadata.google.internal", "metadata.goog");

    /** One fetched resource. {@code body} is capped at {@link #MAX_BODY_BYTES}. */
    public record FetchedPage(
            URI requestedUri,
            URI finalUri,
            int statusCode,
            long durationMs,
            String contentType,
            String body) {}

    private final HostResolver resolver;

    public WebsiteFetcher() {
        this(HostResolver.jdk());
    }

    WebsiteFetcher(HostResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Fetches the given URI. Returns empty when the URL is blocked, the host
     * cannot be resolved, any resolved address is disallowed (fail-closed),
     * or the connection fails at the network level. Non-2xx HTTP responses
     * are returned, not empty. The connection is pinned to the first validated
     * address — no re-resolution happens at connect time (ADR-010).
     */
    public Optional<FetchedPage> fetch(URI uri) {
        if (!isAllowedTarget(uri)) {
            return Optional.empty();
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (BLOCKED_HOSTS.contains(host)) {
            return Optional.empty();
        }
        InetAddress pinned;
        try {
            InetAddress[] addresses = resolver.resolve(host);
            if (addresses == null || addresses.length == 0) {
                return Optional.empty();
            }
            pinned = addresses[0];
            for (InetAddress address : addresses) {
                if (isBlockedAddress(address)) {
                    return Optional.empty();
                }
            }
        } catch (UnknownHostException e) {
            return Optional.empty();
        }
        boolean https = "https".equalsIgnoreCase(uri.getScheme());
        int port = uri.getPort() != -1 ? uri.getPort() : (https ? 443 : 80);
        long start = System.currentTimeMillis();
        try (Socket plain = new Socket()) {
            plain.connect(new InetSocketAddress(pinned, port), CONNECT_TIMEOUT_MS);
            plain.setSoTimeout(REQUEST_TIMEOUT_MS);
            Socket socket = plain;
            if (https) {
                Socket sslSocket;
                try {
                    sslSocket = SSLContext.getDefault().getSocketFactory()
                            .createSocket(plain, host, port, true);
                } catch (java.security.NoSuchAlgorithmException e) {
                    throw new IOException("No default SSLContext", e);
                }
                SSLSocket ssl = (SSLSocket) sslSocket;
                SSLParameters params = ssl.getSSLParameters();
                params.setEndpointIdentificationAlgorithm("HTTPS");
                ssl.setSSLParameters(params);
                ssl.startHandshake();
                socket = ssl;
            }
            String target = uri.getRawPath() == null || uri.getRawPath().isBlank()
                    ? "/" : uri.getRawPath();
            if (uri.getRawQuery() != null) {
                target += "?" + uri.getRawQuery();
            }
            String hostHeader = (port == 80 || port == 443) ? host : host + ":" + port;
            String request = "GET " + target + " HTTP/1.1\r\n"
                    + "Host: " + hostHeader + "\r\n"
                    + "User-Agent: " + USER_AGENT + "\r\n"
                    + "Accept: text/html,application/xhtml+xml,*/*;q=0.5\r\n"
                    + "Connection: close\r\n\r\n";
            OutputStream out = socket.getOutputStream();
            out.write(request.getBytes(StandardCharsets.ISO_8859_1));
            out.flush();
            long duration = System.currentTimeMillis() - start;
            FetchedPage page = readResponse(uri,
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1),
                    duration);
            return Optional.ofNullable(page);
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    private static FetchedPage readResponse(URI uri, InputStreamReader rawReader, long duration)
            throws IOException {
        BufferedReader reader = new BufferedReader(rawReader);
        String statusLine = reader.readLine();
        if (statusLine == null || !statusLine.startsWith("HTTP/")) {
            return null;
        }
        int statusCode = 0;
        String[] parts = statusLine.split(" ");
        if (parts.length >= 2) {
            try {
                statusCode = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        Map<String, String> headers = new LinkedHashMap<>();
        String line;
        while ((line = reader.readLine()) != null && !line.isBlank()) {
            int colon = line.indexOf(':');
            if (colon > 0) {
                headers.putIfAbsent(
                        line.substring(0, colon).trim().toLowerCase(Locale.ROOT),
                        line.substring(colon + 1).trim());
            }
        }
        String contentType = headers.getOrDefault("content-type", "");
        boolean chunked = headers.getOrDefault("transfer-encoding", "")
                .toLowerCase(Locale.ROOT).contains("chunked");
        String rawBody = chunked ? readChunked(reader) : readLimited(reader);
        Charset charset = charsetFrom(contentType);
        String body = new String(rawBody.getBytes(StandardCharsets.ISO_8859_1), charset);
        return new FetchedPage(uri, uri, statusCode, duration, contentType, body);
    }

    /** Reads a chunked body, capped at {@link #MAX_BODY_BYTES}. */
    private static String readChunked(BufferedReader reader) throws IOException {
        StringBuilder body = new StringBuilder();
        while (body.length() < MAX_BODY_BYTES) {
            String sizeLine = reader.readLine();
            if (sizeLine == null) {
                break;
            }
            int semi = sizeLine.indexOf(';');
            int size;
            try {
                size = Integer.parseInt((semi > 0 ? sizeLine.substring(0, semi) : sizeLine).trim(), 16);
            } catch (NumberFormatException e) {
                break;
            }
            if (size <= 0) {
                break;
            }
            int remaining = Math.min(size, MAX_BODY_BYTES - body.length());
            char[] buffer = new char[remaining];
            int read = reader.read(buffer, 0, remaining);
            if (read <= 0) {
                break;
            }
            body.append(buffer, 0, read);
            if (read == size) {
                reader.readLine();
            }
        }
        return body.toString();
    }

    /** Reads a plain body (Content-Length or EOF), capped at {@link #MAX_BODY_BYTES}. */
    private static String readLimited(BufferedReader reader) throws IOException {
        StringBuilder body = new StringBuilder(MAX_BODY_BYTES);
        char[] buffer = new char[8192];
        int read;
        while (body.length() < MAX_BODY_BYTES && (read = reader.read(buffer, 0,
                Math.min(buffer.length, MAX_BODY_BYTES - body.length()))) > 0) {
            body.append(buffer, 0, read);
        }
        return body.toString();
    }

    /** Builds {@code base} with {@code path}, keeping scheme and host. */
    public static URI sibling(URI base, String path) {
        StringBuilder sb = new StringBuilder();
        sb.append(base.getScheme()).append("://").append(base.getHost());
        int port = base.getPort();
        if (port != -1 && port != 80 && port != 443) {
            sb.append(':').append(port);
        }
        sb.append(path);
        return URI.create(sb.toString());
    }

    static boolean isAllowedTarget(URI uri) {
        if (uri == null || uri.getHost() == null) {
            return false;
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            return false;
        }
        if (!ALLOWED_PORTS.contains(uri.getPort())) {
            return false;
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (isBlockedAddress(address)) {
                    return false;
                }
            }
        } catch (UnknownHostException e) {
            return false;
        }
        return true;
    }

    static boolean isBlockedAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;
            // 100.64.0.0/10 carrier-grade NAT
            if (b0 == 100 && b1 >= 64 && b1 <= 127) {
                return true;
            }
            // 0.0.0.0/8 residual "this network"
            if (b0 == 0) {
                return true;
            }
        } else if (bytes.length == 16) {
            // fc00::/7 unique local addresses
            if ((bytes[0] & 0xFE) == 0xFC) {
                return true;
            }
        }
        return false;
    }

    private static Charset charsetFrom(String contentType) {
        if (contentType != null) {
            String lower = contentType.toLowerCase(Locale.ROOT);
            int idx = lower.indexOf("charset=");
            if (idx >= 0) {
                String name = contentType.substring(idx + 8).trim();
                int semi = name.indexOf(';');
                if (semi >= 0) {
                    name = name.substring(0, semi).trim();
                }
                name = name.replace("\"", "");
                try {
                    return Charset.forName(name);
                } catch (RuntimeException ignored) {
                    // fall through to UTF-8
                }
            }
        }
        return StandardCharsets.UTF_8;
    }
}
