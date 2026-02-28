package com.smartinterview.common;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class Result<T> implements Serializable {
    private int code;
    private String msg;
    private T data;
    public static <T> Result<T> success(){
        return success(null);
    }
    //只返回data
    public static <T> Result<T> success(T data){
        Result<T> result=new Result<T>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMsg(ResultCode.SUCCESS.getMsg());
        result.setData(data);
        return result;
    }
    //自定义msg
    public static <T> Result<T> success(T data,String msg){
        Result<T> result=new Result<T>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMsg(msg);
        result.setData(data);
        return result;
    }
    public static <T> Result<T> error(ResultCode resultCode){
        Result<T> result=new Result<T>();
        result.setCode(resultCode.getCode());
        result.setMsg(resultCode.getMsg());
        return result;
    }
    public static <T> Result<T> error(int code,String msg) {
        Result<T> result = new Result<T>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }
}
