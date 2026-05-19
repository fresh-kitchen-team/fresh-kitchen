package com.example.freshkitchen.application.tips.service;

import com.example.freshkitchen.application.analytics.dto.DisplayCategory;
import com.example.freshkitchen.application.tips.usecase.GetStorageTipsUseCase;
import com.example.freshkitchen.domain.tips.entity.StorageTip;
import com.example.freshkitchen.domain.tips.repository.StorageTipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetStorageTipsService implements GetStorageTipsUseCase {

    private final StorageTipRepository storageTipRepository;

    @Override
    public List<StorageTipResult> get(Query query) {
        List<StorageTip> tips;

        if (query.displayCategory() != null) {
            tips = storageTipRepository.findByCategoryInOrderById(
                    query.displayCategory().catalogCategories()
            );
        } else {
            tips = storageTipRepository.findAll(Sort.by("id"));
        }

        return tips.stream()
                .map(this::toResult)
                .toList();
    }

    private StorageTipResult toResult(StorageTip tip) {
        DisplayCategory dc = DisplayCategory.from(tip.getCategory());
        return new StorageTipResult(
                tip.getId(),
                dc,
                dc.displayName(),
                tip.getName(),
                tip.getEmoji(),
                tip.getTip(),
                tip.getStorageType()
        );
    }
}
