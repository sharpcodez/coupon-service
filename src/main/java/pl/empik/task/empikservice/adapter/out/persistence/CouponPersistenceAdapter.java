package pl.empik.task.empikservice.adapter.out.persistence;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.empik.task.empikservice.domain.exception.CouponAlreadyRedeemedException;
import pl.empik.task.empikservice.domain.exception.CouponExhaustedException;
import pl.empik.task.empikservice.domain.exception.CouponNotFoundException;
import pl.empik.task.empikservice.domain.exception.DuplicateCouponCodeException;
import pl.empik.task.empikservice.domain.model.Coupon;
import pl.empik.task.empikservice.domain.model.CouponCode;
import pl.empik.task.empikservice.domain.model.Redemption;
import pl.empik.task.empikservice.domain.model.UserId;
import pl.empik.task.empikservice.domain.port.out.CouponRepository;

import java.time.Instant;
import java.util.Optional;

@Component
class CouponPersistenceAdapter implements CouponRepository {

    private final CouponEntityRepository repository;

    CouponPersistenceAdapter(CouponEntityRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Coupon> findByCode(CouponCode code) {
        return repository.findByCode(code.value()).map(CouponEntity::toDomain);
    }

    @Override
    public Coupon save(Coupon coupon) {
        try {
            return repository.save(CouponEntity.fromDomain(coupon)).toDomain();
        } catch (DuplicateKeyException e) {
            throw new DuplicateCouponCodeException(coupon.code(), e);
        }
    }

    @Transactional
    @Override
    public Redemption recordRedemption(CouponCode code, UserId userId, Instant redeemedAt) {
        try {
            int inserted = repository.insertRedemption(code.value(), userId.value(), redeemedAt);
            if (inserted == 0) {
                throw new CouponNotFoundException(code);
            }
        } catch (DuplicateKeyException e) {
            throw new CouponAlreadyRedeemedException(code, userId, e);
        }
        int updated = repository.incrementUsagesIfAvailable(code.value());
        if (updated == 0) {
            throw new CouponExhaustedException(code);
        }
        return new Redemption(code, userId, redeemedAt);
    }
}
