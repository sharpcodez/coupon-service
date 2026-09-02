package pl.empik.task.empikservice.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class UserIdTest {

    @Test
    void keepsValueVerbatimAfterTrimming() {
        assertThat(UserId.of(" alice ").value()).isEqualTo("alice");
        assertThat(UserId.of("Alice")).isNotEqualTo(UserId.of("alice"));
    }

    @Test
    void rejectsBlankAndNull() {
        assertThatIllegalArgumentException().isThrownBy(() -> UserId.of("  "));
        assertThatNullPointerException().isThrownBy(() -> UserId.of(null));
    }

    @Test
    void rejectsIdLongerThan128Chars() {
        assertThatIllegalArgumentException().isThrownBy(() -> UserId.of("u".repeat(129)));
        assertThat(UserId.of("u".repeat(128)).value()).hasSize(128);
    }

    @Test
    void toStringReturnsTheRawValue() {
        assertThat(UserId.of(" alice ").toString()).isEqualTo("alice");
    }
}
