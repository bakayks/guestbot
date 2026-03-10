package com.guestbot.repository;

import com.guestbot.core.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {

    List<KnowledgeBase> findByHotelId(Long hotelId);

    List<KnowledgeBase> findByHotelIdAndCategory(Long hotelId, String category);

    // Простой текстовый поиск для RAG
    @Query("""
        SELECT k FROM KnowledgeBase k
        WHERE k.hotel.id = :hotelId
        AND (LOWER(k.question) LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(k.answer) LIKE LOWER(CONCAT('%', :query, '%')))
    """)
    List<KnowledgeBase> searchByHotelIdAndQuery(
        @Param("hotelId") Long hotelId,
        @Param("query") String query
    );
}
