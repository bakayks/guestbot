package com.guestbot.core.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "knowledge_base")
@Getter @Setter @NoArgsConstructor
public class KnowledgeBase extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;
}
