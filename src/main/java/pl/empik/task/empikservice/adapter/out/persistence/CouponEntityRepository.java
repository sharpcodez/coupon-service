package pl.empik.task.empikservice.adapter.out.persistence;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

interface CouponEntityRepository extends CrudRepository<CouponEntity, Long> {

    Optional<CouponEntity> findByCode(String code);

    @Modifying
    @Query("""
            UPDATE coupon SET current_usages = current_usages + 1
            WHERE code = :code AND current_usages < max_usages
            """)
    int incrementUsagesIfAvailable(@Param("code") String code);

    @Modifying
    @Query("""
            INSERT INTO redemption (coupon_id, user_id, redeemed_at)
            SELECT c.id, :userId, :redeemedAt FROM coupon c WHERE c.code = :code
            """)
    int insertRedemption(@Param("code") String code,
                         @Param("userId") String userId,
                         @Param("redeemedAt") Instant redeemedAt);
}
