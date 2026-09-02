package pl.empik.task.empikservice.adapter.in.rest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private static final String PEER = "203.0.113.9";

    @Test
    void withNoTrustedProxiesTheHeaderIsCompletelyIgnored() {
        ClientIpResolver resolver = new ClientIpResolver(List.of());

        assertThat(resolver.resolve(PEER, "8.8.8.8")).isEqualTo(PEER);
        assertThat(resolver.resolve(PEER, null)).isEqualTo(PEER);
    }

    @Test
    void headerFromAnUntrustedPeerIsIgnored() {
        ClientIpResolver resolver = new ClientIpResolver(List.of("10.0.0.0/8"));

        assertThat(resolver.resolve(PEER, "8.8.8.8")).isEqualTo(PEER);
    }

    @Test
    void trustedPeerYieldsTheForwardedClient() {
        ClientIpResolver resolver = new ClientIpResolver(List.of("10.0.0.0/8"));

        assertThat(resolver.resolve("10.0.0.1", "8.8.8.8")).isEqualTo("8.8.8.8");
    }

    @Test
    void chainIsWalkedRightToLeftSkippingTrustedProxies() {
        ClientIpResolver resolver = new ClientIpResolver(List.of("10.0.0.0/8"));

        assertThat(resolver.resolve("10.0.0.1", "1.2.3.4, 8.8.8.8, 10.0.0.2")).isEqualTo("8.8.8.8");
    }

    @Test
    void allHopsTrustedFallsBackToPeer() {
        ClientIpResolver resolver = new ClientIpResolver(List.of("10.0.0.0/8"));

        assertThat(resolver.resolve("10.0.0.1", "10.0.0.3, 10.0.0.2")).isEqualTo("10.0.0.1");
    }

    @Test
    void malformedHopDistrustsTheWholeHeader() {
        ClientIpResolver resolver = new ClientIpResolver(List.of("10.0.0.0/8"));

        assertThat(resolver.resolve("10.0.0.1", "garbage")).isEqualTo("10.0.0.1");
        assertThat(resolver.resolve("10.0.0.1", "8.8.8.8, garbage")).isEqualTo("10.0.0.1");
        assertThat(resolver.resolve("10.0.0.1", "   ")).isEqualTo("10.0.0.1");
    }

    @Test
    void blankEntriesInTheConfiguredListAreTolerated() {
        ClientIpResolver resolver = new ClientIpResolver(List.of(" ", "10.0.0.0/8", ""));

        assertThat(resolver.resolve("10.0.0.1", "8.8.8.8")).isEqualTo("8.8.8.8");
    }

    @Test
    void ipv6LoopbackProxyWorks() {
        ClientIpResolver resolver = new ClientIpResolver(List.of("::1/128", "127.0.0.1/32"));

        assertThat(resolver.resolve("::1", "2001:4860:4860::8888")).isEqualTo("2001:4860:4860:0:0:0:0:8888");
        assertThat(resolver.resolve("127.0.0.1", "8.8.8.8")).isEqualTo("8.8.8.8");
    }

    @Test
    void portSuffixesEmittedBySomeProxiesAreStripped() {
        ClientIpResolver resolver = new ClientIpResolver(List.of("10.0.0.0/8"));

        assertThat(resolver.resolve("10.0.0.1", "8.8.8.8:1234")).isEqualTo("8.8.8.8");
        assertThat(resolver.resolve("10.0.0.1", "[2001:db8::1]:9")).isEqualTo("2001:db8:0:0:0:0:0:1");
    }

    @Test
    void emptyHopsFromDoubledCommasAreDroppedRatherThanTreatedAsAddresses() {
        ClientIpResolver resolver = new ClientIpResolver(List.of("10.0.0.0/8"));

        assertThat(resolver.resolve("10.0.0.1", "8.8.8.8,, 10.0.0.2")).isEqualTo("8.8.8.8");
    }

    @Test
    void headerOfOnlyCommasHasNoRealHopsAndFallsBackToPeer() {
        ClientIpResolver resolver = new ClientIpResolver(List.of("10.0.0.0/8"));

        assertThat(resolver.resolve("10.0.0.1", ",")).isEqualTo("10.0.0.1");
    }
}
