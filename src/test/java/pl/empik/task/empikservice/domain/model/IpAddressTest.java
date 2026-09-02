package pl.empik.task.empikservice.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class IpAddressTest {

    @ParameterizedTest
    @ValueSource(strings = {"8.8.8.8", "1.1.1.1", "2001:4860:4860::8888"})
    void publicAddressesArePublic(String literal) {
        assertThat(IpAddress.of(literal).isPublic()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "127.0.0.1",
            "10.1.2.3",
            "172.16.0.1",
            "192.168.1.1",
            "169.254.10.10",
            "0.0.0.0",
            "224.0.0.1",
            "::1",
            "fe80::1",
            "fd12:3456:789a::1"
    })
    void privateAndReservedAddressesAreNotPublic(String literal) {
        assertThat(IpAddress.of(literal).isPublic()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-an-ip", "999.1.1.1", "1.2.3.4.5", ""})
    void rejectsInvalidLiterals(String literal) {
        assertThatIllegalArgumentException().isThrownBy(() -> IpAddress.of(literal));
    }

    @Test
    void literalRoundTrips() {
        assertThat(IpAddress.of(" 8.8.8.8 ").literal()).isEqualTo("8.8.8.8");
    }

    @Test
    void toStringReturnsTheLiteral() {
        IpAddress address = IpAddress.of("8.8.8.8");
        assertThat(address.toString()).isEqualTo(address.literal());
    }
}
