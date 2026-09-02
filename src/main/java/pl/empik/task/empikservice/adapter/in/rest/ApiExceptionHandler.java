package pl.empik.task.empikservice.adapter.in.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import pl.empik.task.empikservice.adapter.in.rest.auth.InvalidCredentialsException;
import pl.empik.task.empikservice.domain.exception.CountryUnresolvableException;
import pl.empik.task.empikservice.domain.exception.CouponAlreadyRedeemedException;
import pl.empik.task.empikservice.domain.exception.CouponExhaustedException;
import pl.empik.task.empikservice.domain.exception.CouponNotFoundException;
import pl.empik.task.empikservice.domain.exception.CouponNotValidInCountryException;
import pl.empik.task.empikservice.domain.exception.DuplicateCouponCodeException;
import pl.empik.task.empikservice.domain.exception.GeoLocationUnavailableException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail invalidCredentials(InvalidCredentialsException e) {
        return problem(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid username or password.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validationFailed(MethodArgumentNotValidException e) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Request validation failed.");
        problem.setProperty("errors", e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .toList());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidArgument(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail malformedBody(HttpMessageNotReadableException e) {
        return problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request body is missing or malformed.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail dataConflict(DataIntegrityViolationException e) {
        log.warn("data integrity violation reached the API layer", e);
        return problem(HttpStatus.CONFLICT, "DATA_CONFLICT", "The request conflicts with existing data.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ProblemDetail methodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "HTTP method not supported for this resource.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ProblemDetail unsupportedMediaType(HttpMediaTypeNotSupportedException e) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "Request content type is not supported.");
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    ProblemDetail notAcceptable(HttpMediaTypeNotAcceptableException e) {
        return problem(HttpStatus.NOT_ACCEPTABLE, "NOT_ACCEPTABLE",
                "Cannot produce a response matching the Accept header.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail resourceNotFound(NoResourceFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "No resource at this path.");
    }

    @ExceptionHandler(CouponNotFoundException.class)
    ProblemDetail couponNotFound(CouponNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "COUPON_NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(DuplicateCouponCodeException.class)
    ProblemDetail duplicateCode(DuplicateCouponCodeException e) {
        return problem(HttpStatus.CONFLICT, "DUPLICATE_COUPON_CODE", e.getMessage());
    }

    @ExceptionHandler(CouponExhaustedException.class)
    ProblemDetail exhausted(CouponExhaustedException e) {
        return problem(HttpStatus.CONFLICT, "COUPON_EXHAUSTED", e.getMessage());
    }

    @ExceptionHandler(CouponAlreadyRedeemedException.class)
    ProblemDetail alreadyRedeemed(CouponAlreadyRedeemedException e) {
        return problem(HttpStatus.CONFLICT, "COUPON_ALREADY_REDEEMED", e.getMessage());
    }

    @ExceptionHandler(CouponNotValidInCountryException.class)
    ProblemDetail wrongCountry(CouponNotValidInCountryException e) {
        return problem(HttpStatus.FORBIDDEN, "COUPON_NOT_VALID_IN_COUNTRY", e.getMessage());
    }

    @ExceptionHandler(CountryUnresolvableException.class)
    ProblemDetail countryUnresolvable(CountryUnresolvableException e) {
        return problem(HttpStatus.FORBIDDEN, "COUNTRY_UNRESOLVABLE", e.getMessage());
    }

    @ExceptionHandler(GeoLocationUnavailableException.class)
    ProblemDetail geoUnavailable(GeoLocationUnavailableException e) {
        log.warn("geolocation unavailable — failing closed", e);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "GEOLOCATION_UNAVAILABLE",
                "Geolocation is temporarily unavailable; the coupon was not redeemed.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception e) {
        log.error("unhandled exception", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.");
    }

    static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", code);
        return problem;
    }
}
