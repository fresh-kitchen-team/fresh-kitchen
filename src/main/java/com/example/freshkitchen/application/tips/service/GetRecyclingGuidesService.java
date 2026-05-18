package com.example.freshkitchen.application.tips.service;

import com.example.freshkitchen.application.tips.usecase.GetRecyclingGuidesUseCase;
import com.example.freshkitchen.domain.tips.repository.RecyclingGuideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetRecyclingGuidesService implements GetRecyclingGuidesUseCase {

    private final RecyclingGuideRepository recyclingGuideRepository;

    @Override
    public List<RecyclingGuideResult> get() {
        return recyclingGuideRepository.findAll().stream()
                .map(guide -> new RecyclingGuideResult(
                        guide.getId(),
                        guide.getName(),
                        guide.getWasteType(),
                        guide.getDescription()
                ))
                .toList();
    }
}
