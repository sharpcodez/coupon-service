package pl.empik.task.empikservice.adapter.in.rest;

import java.net.InetAddress;

final class CidrRange {

    private final byte[] network;
    private final int prefixLength;

    private CidrRange(byte[] network, int prefixLength) {
        this.network = network;
        this.prefixLength = prefixLength;
    }

    static CidrRange parse(String cidr) {
        String trimmed = cidr.trim();
        String addressPart = trimmed;
        String prefixPart = null;
        int slash = trimmed.indexOf('/');
        if (slash >= 0) {
            addressPart = trimmed.substring(0, slash);
            prefixPart = trimmed.substring(slash + 1);
        }
        byte[] address = InetAddress.ofLiteral(addressPart).getAddress();
        int maxPrefix = address.length * 8;
        int prefixLength = prefixPart == null ? maxPrefix : Integer.parseInt(prefixPart);
        if (prefixLength < 0 || prefixLength > maxPrefix) {
            throw new IllegalArgumentException("invalid prefix length in CIDR: " + cidr);
        }
        return new CidrRange(address, prefixLength);
    }

    boolean contains(InetAddress candidate) {
        byte[] bytes = candidate.getAddress();
        if (bytes.length != network.length) {
            return false;
        }
        int fullBytes = prefixLength / 8;
        for (int i = 0; i < fullBytes; i++) {
            if (bytes[i] != network[i]) {
                return false;
            }
        }
        int remainingBits = prefixLength % 8;
        if (remainingBits == 0) {
            return true;
        }
        int mask = (0xFF << (8 - remainingBits)) & 0xFF;
        return (bytes[fullBytes] & mask) == (network[fullBytes] & mask);
    }
}
