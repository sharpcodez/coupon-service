package pl.empik.task.empikservice.domain.model;

import java.net.InetAddress;
import java.util.Objects;

public record IpAddress(InetAddress address) {

    public IpAddress {
        Objects.requireNonNull(address, "ip address must not be null");
    }

    public static IpAddress of(String literal) {
        Objects.requireNonNull(literal, "ip address must not be null");
        try {
            return new IpAddress(InetAddress.ofLiteral(literal.trim()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("not a valid IP address literal", e);
        }
    }

    public boolean isPublic() {
        return !(address.isLoopbackAddress()
                || address.isSiteLocalAddress()
                || address.isLinkLocalAddress()
                || address.isAnyLocalAddress()
                || address.isMulticastAddress()
                || isUniqueLocalIpv6());
    }

    private boolean isUniqueLocalIpv6() {
        byte[] bytes = address.getAddress();
        return bytes.length == 16 && (bytes[0] & 0xFE) == 0xFC;
    }

    public String literal() {
        return address.getHostAddress();
    }

    @Override
    public String toString() {
        return literal();
    }
}
