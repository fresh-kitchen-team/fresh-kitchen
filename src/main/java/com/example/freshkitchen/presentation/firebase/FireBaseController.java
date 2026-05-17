package com.example.freshkitchen.presentation.firebase;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자 알림 및 설정", description = "사용자 기기 토큰 및 알림 설정 관련 API")
@RestController
@RequestMapping("/api/v1/user/notification")
@RequiredArgsConstructor
public class FireBaseController {



}
