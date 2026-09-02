package pl.empik.task.empikservice.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class CouponCodeTest {

    @Test
    void normalizesToUppercaseSoCaseInsensitiveCodesAreEqual() {
        assertThat(CouponCode.of("wiosna")).isEqualTo(CouponCode.of("WIOSNA"));
        assertThat(CouponCode.of("WiOsNa").value()).isEqualTo("WIOSNA");
    }

    @Test
    void trimsSurroundingWhitespace() {
        assertThat(CouponCode.of("  lato  ").value()).isEqualTo("LATO");
    }

    @Test
    void rejectsBlank() {
        assertThatIllegalArgumentException().isThrownBy(() -> CouponCode.of("   "));
        assertThatIllegalArgumentException().isThrownBy(() -> CouponCode.of(""));
    }

    @Test
    void rejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> CouponCode.of(null));
    }

    @Test
    void rejectsCodeLongerThan64Chars() {
        assertThatIllegalArgumentException().isThrownBy(() -> CouponCode.of("X".repeat(65)));
        assertThat(CouponCode.of("X".repeat(64)).value()).hasSize(64);
    }

    @Test
    void allowsOnlyUriSafeCharacters() {
        assertThat(CouponCode.of("lato-2026_b").value()).isEqualTo("LATO-2026_B");
        assertThatIllegalArgumentException().isThrownBy(() -> CouponCode.of("A B"));
        assertThatIllegalArgumentException().isThrownBy(() -> CouponCode.of("WIO/SNA"));
        assertThatIllegalArgumentException().isThrownBy(() -> CouponCode.of("kupon#1"));
    }
}
