package org.example.backend.administrator.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.user.model.UserDto;
import org.example.backend.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/administrator/users", "/admin/users"})
public class AdministratorUserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody UserDto.CreateUserReq dto) {
        UserDto.CreateUserRes result = userService.createUserByAdmin(dto);
        return ResponseEntity.ok(BaseResponse.success(result));
    }
}
