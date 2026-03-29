package com.smartinterview.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String nickname;
    private Integer role;
    private String phone;
}
