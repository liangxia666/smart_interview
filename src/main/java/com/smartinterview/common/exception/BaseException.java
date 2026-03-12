package com.smartinterview.common.exception;

import com.smartinterview.common.result.ResultCode;
import lombok.Data;
import lombok.Getter;

@Getter
public class BaseException extends RuntimeException{
    private  int code;
    public BaseException(){}

    public BaseException(ResultCode resultCode){
        super(resultCode.getMsg());
        this.code= resultCode.getCode();

    }
    public BaseException(String msg){
        super(msg);
        this.code=ResultCode.BUSINESS_ERROR.getCode();
    }
    public BaseException(int code,String msg){
        super(msg);
        this.code=code;
    }
}
