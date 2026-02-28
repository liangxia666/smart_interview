package com.smartinterview.handler;

import com.smartinterview.common.exception.BaseException;
import com.smartinterview.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalHandlerException {
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.info(ex.getMessage());
        return Result.error(ex);
    }
}
