package com.example.freshkitchen.global.service;

import com.example.freshkitchen.global.exception.dto.ImageDto;
import com.example.freshkitchen.global.exception.dto.PresignedUrlRequest;
import com.example.freshkitchen.global.exception.dto.PresignedUrlResponse;
import jakarta.transaction.Transactional;

import java.util.List;

public interface ImageService {

    List<PresignedUrlResponse> generatePresignedUrls(PresignedUrlRequest request);



    void deleteObject(String keyOrUrl);

    void deleteFolder(String fileLocation);

    /** 요청 키(URL/키)를 검증하고 temp/*면 최종으로 이동하여 Image(USER, userId, order=0)로 upsert. 최종 key 반환 */
    String upsertUserProfileImage(Long userId, String requestedKeyOrUrl);

//    @Transactional
//    String upsertChatRoomProfileImage(Long chatRoomId, String requestedKeyOrUrl);
//
    /** 현재 프로필 이미지를 삭제(S3 + image 레코드) */
    void deleteUserProfileImage(Long userId);


    /** 현재 프로필 이미지 key 조회(없으면 null) */
    String getUserProfileKey(Long userId);

    /** key 또는 URL을 내부 key로 정규화 */
    String normalizeKey(String keyOrUrl);

    /** 내부 key → 공개 URL */
    String toPublicUrl(String keyOrNull);
    List<ImageDto> findImagesForChatRooms(List<Long> roomIds);
}
