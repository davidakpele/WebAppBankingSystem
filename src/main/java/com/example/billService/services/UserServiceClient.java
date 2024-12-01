package com.example.billService.services;

import com.example.billService.dto.UserDTO;

public interface UserServiceClient {
    UserDTO getUserById(Long id, String token);

    UserDTO getUserByUsername(String username, String token);

    UserDTO findUserByUsernameInPublicRoute(String username);

    UserDTO fetchPublicUserById(Long userId);
}
