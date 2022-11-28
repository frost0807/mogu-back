package com.teamof4.mogu.constants;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ResponseConstants {

    public static final ResponseEntity<Void> OK =
            ResponseEntity.ok().build();

    public static final ResponseEntity<Void> CREATED =
            ResponseEntity.status(HttpStatus.CREATED).build();

    public static final ResponseEntity<Void> BAD_REQUEST =
            ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

    public static final ResponseEntity<String> DUPLICATED_EMAIL =
            new ResponseEntity<>("이미 등록된 이메일입니다.", HttpStatus.CONFLICT);

    public static final ResponseEntity<String> DUPLICATED_NICKNAME =
            new ResponseEntity<>("이미 등록된 닉네임입니다.", HttpStatus.CONFLICT);

    public static final ResponseEntity<String> DUPLICATED_PHONE =
            new ResponseEntity<>("이미 등록된 휴대폰 번호입니다", HttpStatus.CONFLICT);

    public static final ResponseEntity<String> USER_NOT_FOUND =
            new ResponseEntity<>("이메일 혹은 비밀번호를 잘못 입력하였습니다.", HttpStatus.NOT_FOUND);


    public static final ResponseEntity<String> WRONG_PASSWORD =
            new ResponseEntity<>("잘못된 비밀번호를 입력하였습니다.", HttpStatus.UNAUTHORIZED);

    public static final ResponseEntity<String> FAILED_IMAGE_CONVERT =
            new ResponseEntity<>("이미지 파일 변환에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    public static final ResponseEntity<String> FAILED_IMAGE_UPLOAD =
            new ResponseEntity<>("이미지 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
}
