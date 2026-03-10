package com.guestbot.service.knowledge;

import com.guestbot.core.entity.Hotel;
import com.guestbot.core.entity.KnowledgeBase;
import com.guestbot.core.exception.ResourceNotFoundException;
import com.guestbot.repository.HotelRepository;
import com.guestbot.repository.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final HotelRepository hotelRepository;

    @Transactional(readOnly = true)
    public List<KnowledgeBase> getByHotel(Long hotelId) {
        return knowledgeBaseRepository.findByHotelId(hotelId);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeBase> search(Long hotelId, String query) {
        return knowledgeBaseRepository.searchByHotelIdAndQuery(hotelId, query);
    }

    @Transactional
    public KnowledgeBase create(Long hotelId, KnowledgeBase kb) {
        Hotel hotel = hotelRepository.findById(hotelId)
            .orElseThrow(() -> new ResourceNotFoundException("Hotel", hotelId));
        kb.setHotel(hotel);
        return knowledgeBaseRepository.save(kb);
    }

    @Transactional
    public KnowledgeBase update(Long id, KnowledgeBase updated) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("KnowledgeBase", id));
        kb.setCategory(updated.getCategory());
        kb.setQuestion(updated.getQuestion());
        kb.setAnswer(updated.getAnswer());
        return knowledgeBaseRepository.save(kb);
    }

    @Transactional
    public void delete(Long id) {
        knowledgeBaseRepository.deleteById(id);
    }

    // Форматирует KB в текст для Claude системного промпта
    public String formatForPrompt(Long hotelId) {
        List<KnowledgeBase> items = getByHotel(hotelId);
        if (items.isEmpty()) return "";

        StringBuilder sb = new StringBuilder("## База знаний:\n");
        String currentCategory = null;

        for (KnowledgeBase item : items) {
            if (!item.getCategory().equals(currentCategory)) {
                currentCategory = item.getCategory();
                sb.append("\n### ").append(currentCategory).append("\n");
            }
            sb.append("В: ").append(item.getQuestion()).append("\n");
            sb.append("О: ").append(item.getAnswer()).append("\n");
        }

        return sb.toString();
    }
}
