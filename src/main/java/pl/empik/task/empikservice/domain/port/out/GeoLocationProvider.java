package pl.empik.task.empikservice.domain.port.out;

import pl.empik.task.empikservice.domain.model.Country;
import pl.empik.task.empikservice.domain.model.IpAddress;

public interface GeoLocationProvider {
    Country resolveCountry(IpAddress ip);
}
