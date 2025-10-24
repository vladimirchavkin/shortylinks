package ru.chavkin.em.linkshortener.exception.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.chavkin.em.linkshortener.entity.ErrorResponse;
import ru.chavkin.em.linkshortener.exception.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AliasAlreadyExistException.class)
    public ResponseEntity<ErrorResponse> handleAliasAlreadyExistException(AliasAlreadyExistException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage(), ex.getErrorCode()));
    }

    @ExceptionHandler(AliasValueException.class)
    public ResponseEntity<ErrorResponse> handleAliasValueException(AliasValueException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage(), ex.getErrorCode()));
    }

    @ExceptionHandler(LinkExpirationDateValueException.class)
    public ResponseEntity<ErrorResponse> handleLinkExpirationDateValueException(LinkExpirationDateValueException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage(), ex.getErrorCode()));
    }

    @ExceptionHandler(LinkNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLinkNotFoundException(LinkNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage(), ex.getErrorCode()));
    }

    @ExceptionHandler(OriginalLinkValueException.class)
    public ResponseEntity<ErrorResponse> handleOriginalLinkValueException(OriginalLinkValueException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage(), ex.getErrorCode()));
    }

    @ExceptionHandler(ShortCodeGenerationMaxAttemptsException.class)
    public ResponseEntity<ErrorResponse> handleShortCodeGenerationMaxAttemptsException(ShortCodeGenerationMaxAttemptsException ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(ex.getMessage(), ex.getErrorCode()));
    }


}
