package pl.empik.task.empikservice.adapter.in.rest;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

public final class ClientIpResolver {

    private final List<CidrRange> trustedProxies;

    public ClientIpResolver(List<String> trustedProxyCidrs) {
        this.trustedProxies = trustedProxyCidrs.stream()
                .filter(cidr -> !cidr.isBlank())
                .map(CidrRange::parse)
                .toList();
    }

    public String resolve(String remoteAddr, String xForwardedFor) {
        if (trustedProxies.isEmpty() || !isTrusted(parseOrNull(remoteAddr))) {
            return remoteAddr;
        }
        if (xForwardedFor == null || xForwardedFor.isBlank()) {
            return remoteAddr;
        }
        List<InetAddress> hops = parseHops(xForwardedFor);
        if (hops == null) {
            return remoteAddr;
        }
        for (int i = hops.size() - 1; i >= 0; i--) {
            InetAddress hop = hops.get(i);
            if (!isTrusted(hop)) {
                return hop.getHostAddress();
            }
        }
        return remoteAddr;
    }

    private List<InetAddress> parseHops(String header) {
        List<String> parts = Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .toList();
        if (parts.isEmpty()) {
            return null;
        }
        List<InetAddress> hops = parts.stream()
                .map(part -> parseOrNull(stripPort(part)))
                .toList();
        return hops.contains(null) ? null : hops;
    }

    private static String stripPort(String hop) {
        if (hop.startsWith("[")) {
            int closing = hop.indexOf(']');
            return closing > 0 ? hop.substring(1, closing) : hop;
        }
        int colon = hop.indexOf(':');
        if (colon >= 0 && hop.indexOf(':', colon + 1) < 0) {
            return hop.substring(0, colon);
        }
        return hop;
    }

    private boolean isTrusted(InetAddress address) {
        return address != null && trustedProxies.stream().anyMatch(range -> range.contains(address));
    }

    private static InetAddress parseOrNull(String literal) {
        try {
            return InetAddress.ofLiteral(literal);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
