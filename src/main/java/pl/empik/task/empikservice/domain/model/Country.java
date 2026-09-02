package pl.empik.task.empikservice.domain.model;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public record Country(String isoCode) {

    private static final Set<String> ISO_COUNTRIES = Set.of(Locale.getISOCountries());

    public Country {
        Objects.requireNonNull(isoCode, "country must not be null");
        isoCode = isoCode.trim().toUpperCase(Locale.ROOT);
        if (!ISO_COUNTRIES.contains(isoCode)) {
            throw new IllegalArgumentException("not an ISO 3166-1 alpha-2 country code: " + isoCode);
        }
    }

    public static Country of(String isoCode) {
        return new Country(isoCode);
    }

    @Override
    public String toString() {
        return isoCode;
    }
}
