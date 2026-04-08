package com.guestbot.api.mapper;

import com.guestbot.api.dto.request.KnowledgeBaseRequest;
import com.guestbot.api.dto.response.KnowledgeBaseResponse;
import com.guestbot.core.entity.KnowledgeBase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface KnowledgeBaseMapper {

    @Mapping(target = "hotelId", source = "hotel.id")
    KnowledgeBaseResponse toResponse(KnowledgeBase kb);

    List<KnowledgeBaseResponse> toList(List<KnowledgeBase> kbs);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    KnowledgeBase toEntity(KnowledgeBaseRequest request);
}
