package pl.empik.task.empikservice.domain.exception;

import pl.empik.task.empikservice.domain.model.IpAddress;

public final class CountryUnresolvableException extends DomainException {

    public CountryUnresolvableException(IpAddress ip) {
        super("country cannot be determined for address " + ip);
    }
}
