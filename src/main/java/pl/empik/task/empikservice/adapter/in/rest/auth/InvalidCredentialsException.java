package pl.empik.task.empikservice.adapter.in.rest.auth;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(Throwable cause) {
        super("invalid username or password", cause);
    }
}
