package in.fixna.platform.websiteaudit;

import java.net.InetAddress;
import java.net.URI;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebsiteFetcherTest {

    @Test
    void blockedAddressRangesAreRejected() throws Exception {
        assertThat(WebsiteFetcher.isBlockedAddress(InetAddress.getByName("127.0.0.1"))).isTrue();
        assertThat(WebsiteFetcher.isBlockedAddress(InetAddress.getByName("10.0.0.1"))).isTrue();
        assertThat(WebsiteFetcher.isBlockedAddress(InetAddress.getByName("172.16.0.1"))).isTrue();
        assertThat(WebsiteFetcher.isBlockedAddress(InetAddress.getByName("192.168.1.1"))).isTrue();
        assertThat(WebsiteFetcher.isBlockedAddress(InetAddress.getByName("169.254.169.254"))).isTrue();
        assertThat(WebsiteFetcher.isBlockedAddress(InetAddress.getByName("100.64.0.1"))).isTrue();
        assertThat(WebsiteFetcher.isBlockedAddress(InetAddress.getByName("0.0.0.1"))).isTrue();
        assertThat(WebsiteFetcher.isBlockedAddress(InetAddress.getByName("::1"))).isTrue();
        assertThat(WebsiteFetcher.isBlockedAddress(InetAddress.getByName("fc00::1"))).isTrue();
        assertThat(WebsiteFetcher.isBlockedAddress(InetAddress.getByName("fe80::1"))).isTrue();
        assertThat(WebsiteFetcher.isBlockedAddress(InetAddress.getByName("ff02::1"))).isTrue();
    }

    @Test
    void publicAddressesAreAllowed() throws Exception {
        assertThat(WebsiteFetcher.isBlockedAddress(InetAddress.getByName("8.8.8.8"))).isFalse();
        assertThat(WebsiteFetcher.isBlockedAddress(InetAddress.getByName("1.1.1.1"))).isFalse();
        assertThat(WebsiteFetcher.isBlockedAddress(InetAddress.getByName("93.184.216.34"))).isFalse();
        assertThat(WebsiteFetcher.isBlockedAddress(InetAddress.getByName("2001:4860:4860::8888"))).isFalse();
    }

    @Test
    void disallowedTargetsAreRejectedBeforeAnyNetworkCall() {
        assertThat(WebsiteFetcher.isAllowedTarget(null)).isFalse();
        assertThat(WebsiteFetcher.isAllowedTarget(URI.create("ftp://example.com/"))).isFalse();
        assertThat(WebsiteFetcher.isAllowedTarget(URI.create("file:///etc/passwd"))).isFalse();
        assertThat(WebsiteFetcher.isAllowedTarget(URI.create("http://example.com:8080/"))).isFalse();
        assertThat(WebsiteFetcher.isAllowedTarget(URI.create("http://127.0.0.1/"))).isFalse();
        assertThat(WebsiteFetcher.isAllowedTarget(URI.create("http://localhost/"))).isFalse();
        assertThat(WebsiteFetcher.isAllowedTarget(URI.create("https://169.254.169.254/latest/meta-data/"))).isFalse();
    }

    @Test
    void allowedTargetAcceptsPublicHttpIpLiteral() {
        assertThat(WebsiteFetcher.isAllowedTarget(URI.create("http://93.184.216.34/"))).isTrue();
        assertThat(WebsiteFetcher.isAllowedTarget(URI.create("https://93.184.216.34/"))).isTrue();
    }

    @Test
    void fetchReturnsEmptyForBlockedTargetsWithoutNetwork() {
        WebsiteFetcher fetcher = new WebsiteFetcher();
        assertThat(fetcher.fetch(URI.create("http://127.0.0.1/"))).isEmpty();
        assertThat(fetcher.fetch(URI.create("http://169.254.169.254/"))).isEmpty();
        assertThat(fetcher.fetch(URI.create("http://10.1.2.3/"))).isEmpty();
    }

    @Test
    void siblingKeepsSchemeHostAndPort() {
        assertThat(WebsiteFetcher.sibling(URI.create("https://example.com/page?x=1"), "/robots.txt"))
                .hasToString("https://example.com/robots.txt");
        assertThat(WebsiteFetcher.sibling(URI.create("http://example.com:8080/page"), "/sitemap.xml"))
                .hasToString("http://example.com:8080/sitemap.xml");
        assertThat(WebsiteFetcher.sibling(URI.create("https://example.com:443/page"), "/robots.txt"))
                .hasToString("https://example.com/robots.txt");
    }
}
