package pl.empik.task.empikservice.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class CountryTest {

    @Test
    void acceptsIsoAlpha2CodesCaseInsensitively() {
        assertThat(Country.of("pl").isoCode()).isEqualTo("PL");
        assertThat(Country.of(" DE ").isoCode()).isEqualTo("DE");
        assertThat(Country.of("pl")).isEqualTo(Country.of("PL"));
    }

    @Test
    void rejectsCodesOutsideIso3166() {
        assertThatIllegalArgumentException().isThrownBy(() -> Country.of("ZZ"));
        assertThatIllegalArgumentException().isThrownBy(() -> Country.of("POL"));
        assertThatIllegalArgumentException().isThrownBy(() -> Country.of("P"));
        assertThatIllegalArgumentException().isThrownBy(() -> Country.of(""));
    }

    @Test
    void rejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> Country.of(null));
    }
}
