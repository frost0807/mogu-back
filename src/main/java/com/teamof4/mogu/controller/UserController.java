package com.teamof4.mogu.controller;

import com.teamof4.mogu.dto.UserDto.SaveRequest;
import com.teamof4.mogu.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

import static com.teamof4.mogu.constants.ResponseConstants.CREATED;
import static com.teamof4.mogu.dto.UserDto.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
@Api(tags = {"01. User API"})
public class UserController {

    private final UserService userService;

    @PostMapping("/create")
    @ApiOperation(value = "회원 등록")
    public ResponseEntity<Void> createUser(@Valid @RequestPart SaveRequest requestDto,
                                           @RequestPart MultipartFile profileImage) {
        userService.save(requestDto, profileImage);

        return CREATED;
    }

    @PostMapping("/login")
    @ApiOperation(value = "로그인")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest requestDto) {

        LoginResponse loginResponse = userService.login(requestDto);

        return ResponseEntity.ok(loginResponse);
    }
}