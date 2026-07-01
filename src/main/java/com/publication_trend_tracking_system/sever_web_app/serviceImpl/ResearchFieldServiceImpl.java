package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.response.ResearchFieldResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.ResearchField;
import com.publication_trend_tracking_system.sever_web_app.repository.ResearchFieldRepository;
import com.publication_trend_tracking_system.sever_web_app.service.ResearchFieldService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResearchFieldServiceImpl implements ResearchFieldService {

    private final ResearchFieldRepository fieldRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ResearchFieldResponse> getAllFields() {
        return fieldRepository.findAll().stream().map(this::toResponse).toList();
    }

    private ResearchFieldResponse toResponse(ResearchField field) {
        return ResearchFieldResponse.builder()
                .fieldId(field.getFieldId())
                .fieldName(field.getFieldName())
                .description(field.getDescription())
                .build();
    }
}
