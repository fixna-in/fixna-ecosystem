package in.fixna.platform.websiteaudit;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Seam for DNS resolution so SSRF and DNS-rebinding behavior is
 * unit-testable (ADR-010). Production uses the JDK resolver; tests
 * substitute deterministic answers (including rebinding simulations).
 */
public interface HostResolver {

    InetAddress[] resolve(String host) throws UnknownHostException;

    static HostResolver jdk() {
        return InetAddress::getAllByName;
    }
}