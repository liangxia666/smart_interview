package com.smartinterview.common.util;


import com.smartinterview.dto.UserDTO;
import com.smartinterview.entity.User;

public class UserHolder {
    private static final ThreadLocal<UserDTO> tl=new ThreadLocal<>();
    public static final void save(UserDTO userDTO){tl.set(userDTO);}
    public static final UserDTO getUser(){return tl.get();}
    public static final void remove(){tl.remove();}
}
