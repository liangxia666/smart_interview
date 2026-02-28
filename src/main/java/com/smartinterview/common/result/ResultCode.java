package com.smartinterview.common;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或Token过期"),
    FORBIDDEN(403, "没有权限"),
    NOT_FOUND(404, "资源不存在"),
    BUSINESS_ERROR(500, "业务异常"),
    SERVER_ERROR(500, "服务器内部错误"),
    // 秒杀专用
    SECKILL_FAILED(600, "秒杀失败"),
    SECKILL_STOCK_EMPTY(601, "库存不足"),
    SECKILL_REPEAT(602, "不能重复抢约"),
    SECKILL_NOT_START(603, "活动未开始"),
    SECKILL_ENDED(604, "活动已结束");
    private final int code;
    private final String msg;
     ResultCode(int code,String msg){
        this.code=code;
        this.msg=msg;
    }
}
