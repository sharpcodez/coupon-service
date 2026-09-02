package pl.empik.task.empikservice.adapter.in.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ProblemJsonAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/problem+json");
        response.getWriter().write("""
                {"type":"about:blank","title":"Forbidden","status":403,\
                "detail":"You do not have permission to perform this operation.",\
                "code":"ACCESS_DENIED"}""");
    }
}
