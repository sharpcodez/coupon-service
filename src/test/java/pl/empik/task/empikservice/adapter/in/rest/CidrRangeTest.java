package pl.empik.task.empikservice.adapter.in.rest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CidrRangeTest {

    private static InetAddress ip(String literal) {
        return InetAddress.ofLiteral(literal);
    }

    @ParameterizedTest
    @CsvSource({
            "10.0.0.0/8,      10.0.0.1,        true",
            "10.0.0.0/8,      10.255.255.255,  true",
            "10.0.0.0/8,      11.0.0.0,        false",
            "192.168.1.0/24,  192.168.1.200,   true",
            "192.168.1.0/24,  192.168.2.1,     false",
            "203.0.113.7/32,  203.0.113.7,     true",
            "203.0.113.7/32,  203.0.113.8,     false",
            "203.0.113.8/29,  203.0.113.15,    true",
            "203.0.113.8/29,  203.0.113.16,    false",
            "0.0.0.0/0,       8.8.8.8,         true",
            "2001:db8::/32,   2001:db8::1,     true",
            "2001:db8::/32,   2001:db9::1,     false",
            "::1/128,         ::1,             true"
    })
    void containsMatchesPrefixBits(String cidr, String candidate, boolean expected) {
        assertThat(CidrRange.parse(cidr).contains(ip(candidate))).isEqualTo(expected);
    }

    @Test
    void bareAddressMeansFullLengthPrefix() {
        assertThat(CidrRange.parse("10.1.2.3").contains(ip("10.1.2.3"))).isTrue();
        assertThat(CidrRange.parse("10.1.2.3").contains(ip("10.1.2.4"))).isFalse();
    }

    @Test
    void familyMismatchNeverMatches() {
        assertThat(CidrRange.parse("0.0.0.0/0").contains(ip("2001:db8::1"))).isFalse();
        assertThat(CidrRange.parse("::/0").contains(ip("8.8.8.8"))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.0.0.0/33", "10.0.0.0/-1", "2001:db8::/129", "banana/8", "10.0.0.0/x"})
    void rejectsInvalidCidrs(String cidr) {
        assertThatIllegalArgumentException().isThrownBy(() -> CidrRange.parse(cidr));
    }
}
