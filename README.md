# Coupon Service

REST service for creating and redeeming discount coupons.

## Running

```bash
docker compose up --build
```

Starts PostgreSQL and the app on `http://localhost:8080`. Check both are healthy with
`docker compose ps`; tear down with `docker compose down -v`.

> If host port 8080 is taken, change `ports: ["8080:8080"]` to e.g. `["18080:8080"]` in
> `docker-compose.yml` and adjust the URLs below.

## Users

Pre-provisioned for the demo, **not for production**:

| Username | Password | Role | Can do |
|---|---|---|---|
| `admin` | `admin123` | `ADMIN` | create and read coupons, redeem |
| `user`  | `user123`  | `USER`  | redeem coupons |

## API

Base path `/api/v1`. Errors are RFC 9457 `application/problem+json` with a stable `code` field.

| Endpoint | Auth | Success | Errors |
|---|---|---|---|
| `POST /auth/token` `{username, password}` | public | 200 `{accessToken, tokenType, expiresIn}` | 401 `INVALID_CREDENTIALS` |
| `POST /coupons` `{code, maxUsages, country}` | ADMIN | 201 + `Location` + representation | 400 `VALIDATION_FAILED`; 409 `DUPLICATE_COUPON_CODE` |
| `GET /coupons/{code}` | ADMIN | 200 representation | 404 `COUPON_NOT_FOUND` |
| `POST /coupons/{code}/redemptions` (empty body) | USER/ADMIN | 201 `{couponCode, userId, redeemedAt}` | 404 `COUPON_NOT_FOUND`; 403 `COUPON_NOT_VALID_IN_COUNTRY`; 403 `COUNTRY_UNRESOLVABLE`; 409 `COUPON_EXHAUSTED`; 409 `COUPON_ALREADY_REDEEMED`; 503 `GEOLOCATION_UNAVAILABLE` |

Coupon representation: `{code, createdAt, maxUsages, currentUsages, country}`.
Codes are case-insensitive and limited to `[A-Za-z0-9_-]`, max 64 chars; `country` is a 2-letter
ISO code; `maxUsages` is 1..1,000,000. Redemption is first-come-first-served up to `maxUsages`,
one redemption per user per coupon, and only from the coupon's target country (resolved from the
caller's IP).

Other responses: 400 `MALFORMED_REQUEST`, 401 `AUTHENTICATION_REQUIRED`, 403 `ACCESS_DENIED`,
405 `METHOD_NOT_ALLOWED`, 406 `NOT_ACCEPTABLE`, 415 `UNSUPPORTED_MEDIA_TYPE`,
409 `DATA_CONFLICT`, 500 `INTERNAL_ERROR`.

### Example session (requires `jq`)

```bash
ADMIN_TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/token \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r .accessToken)

# create — 201
curl -si -X POST localhost:8080/api/v1/coupons \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json' \
  -d '{"code":"WIOSNA","maxUsages":3,"country":"PL"}' | head -1

USER_TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/token \
  -H 'Content-Type: application/json' \
  -d '{"username":"user","password":"user123"}' | jq -r .accessToken)

# redeem (lowercase code works) — 201
curl -si -X POST localhost:8080/api/v1/coupons/wiosna/redemptions \
  -H "Authorization: Bearer $USER_TOKEN" | head -1

# same user again — 409 COUPON_ALREADY_REDEEMED
curl -s -X POST localhost:8080/api/v1/coupons/wiosna/redemptions \
  -H "Authorization: Bearer $USER_TOKEN" | jq .code

# no token — 401
curl -si -X POST localhost:8080/api/v1/coupons \
  -H 'Content-Type: application/json' -d '{"code":"X","maxUsages":1,"country":"PL"}' | head -1
```