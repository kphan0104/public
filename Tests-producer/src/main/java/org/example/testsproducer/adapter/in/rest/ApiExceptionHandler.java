package org.example.testsproducer.adapter.in.rest;

import org.example.testsproducer.adapter.out.json.EventSerializationException;
import org.example.testsproducer.application.exception.EventPublicationException;
import org.example.testsproducer.application.exception.MessageTooLargeException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(
                        error.getField(),
                        error.getDefaultMessage()
                )
        );
        return validationProblem(errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ProblemDetail handleMethodValidation(
            HandlerMethodValidationException exception
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getParameterValidationResults().forEach(result -> {
            if (result instanceof ParameterErrors parameterErrors) {
                parameterErrors.getFieldErrors().forEach(error ->
                        errors.putIfAbsent(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );
            } else {
                addParameterErrors(errors, result);
            }
        });
        return validationProblem(errors);
    }

    private void addParameterErrors(
            Map<String, String> errors,
            ParameterValidationResult result
    ) {
        String parameterName = result.getMethodParameter().getParameterName();
        if (parameterName == null) {
            parameterName = "parameter";
        }
        String finalParameterName = parameterName;
        result.getResolvableErrors().forEach(error ->
                errors.putIfAbsent(
                        finalParameterName,
                        error.getDefaultMessage()
                )
        );
    }

    private ProblemDetail validationProblem(Map<String, String> errors) {
        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "Requête invalide",
                "Un ou plusieurs champs sont invalides"
        );
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(MessageTooLargeException.class)
    ProblemDetail handleMessageTooLarge(MessageTooLargeException exception) {
        return problem(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Message trop volumineux",
                exception.getMessage()
        );
    }

    @ExceptionHandler(UnknownFlowException.class)
    ProblemDetail handleUnknownFlow(UnknownFlowException exception) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Flux inconnu",
                exception.getMessage()
        );
    }

    @ExceptionHandler(EventPublicationException.class)
    ProblemDetail handleKafka(EventPublicationException exception) {
        return problem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Kafka indisponible",
                exception.getMessage()
        );
    }

    @ExceptionHandler(EventSerializationException.class)
    ProblemDetail handleSerialization(
            EventSerializationException exception
    ) {
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erreur de sérialisation",
                exception.getMessage()
        );
    }

    private ProblemDetail problem(
            HttpStatus status,
            String title,
            String detail
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                status,
                detail
        );
        problem.setTitle(title);
        problem.setType(URI.create("about:blank"));
        return problem;
    }
}
