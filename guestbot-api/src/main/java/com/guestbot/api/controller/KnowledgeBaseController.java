package com.guestbot.api.controller;

import com.guestbot.core.entity.KnowledgeBase;
import com.guestbot.service.knowledge.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hotels/{hotelId}/knowledge-base")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @GetMapping
    public ResponseEntity<List<KnowledgeBase>> getAll(@PathVariable Long hotelId) {
        return ResponseEntity.ok(knowledgeBaseService.getByHotel(hotelId));
    }

    @PostMapping
    public ResponseEntity<KnowledgeBase> create(
        @PathVariable Long hotelId,
        @RequestBody KnowledgeBase kb
    ) {
        return ResponseEntity.ok(knowledgeBaseService.create(hotelId, kb));
    }

    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeBase> update(
        @PathVariable Long id,
        @RequestBody KnowledgeBase kb
    ) {
        return ResponseEntity.ok(knowledgeBaseService.update(id, kb));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        knowledgeBaseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
