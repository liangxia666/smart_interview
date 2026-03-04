package com.smartinterview.dto;

import lombok.Data;


@Data
public class LoginDTO {
    private String phone;
    private String code;
    private String password;
}
