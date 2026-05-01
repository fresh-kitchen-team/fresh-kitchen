package com.example.freshkitchen.domain.chat.controller;


import com.example.freshkitchen.domain.chat.service.ETLPipelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "AI 필요한 벡터저장소 ", description = "만개의 레시피에 필요한 데이터 임베딩해서 VectorStore type=recipe로 저장 ")
@RestController
@Slf4j
@RequestMapping
@RequiredArgsConstructor
public class AddVectorController {

    private final ETLPipelineService etlpipelineService;

    @Operation(summary = "AI에 필요한 벡터 저장소", description = "AI와 1:1 채팅방을 생성합니다. 기존 방이 있으면 isNew: false, 새로 생성되면 isNew: true를 반환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "벡터 저장 성공",
                    content = @Content(schema = @Schema(implementation = String.class))) // String으로 수정
    })
    @PostMapping("/add-vector-store")
    public String addDocument(
            @RequestParam(value = "attach", required = false) MultipartFile attach) throws IOException {
        log.info("addDocument file={}", attach.getOriginalFilename());
        return etlpipelineService.addVectorStore(attach);
    }
}