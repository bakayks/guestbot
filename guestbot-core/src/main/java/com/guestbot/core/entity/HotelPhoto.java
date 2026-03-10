package com.guestbot.core.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hotel_photos")
@Getter @Setter @NoArgsConstructor
public class HotelPhoto extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(nullable = false)
    private String url;

    private String minioKey;
    private Integer sortOrder = 0;
}
