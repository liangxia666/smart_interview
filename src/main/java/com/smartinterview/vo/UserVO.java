package com.smartinterview.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserVO {
    //雪花算法防止前端精度丢失
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String nickname;
    private String avatar;
    private String phone;
}
