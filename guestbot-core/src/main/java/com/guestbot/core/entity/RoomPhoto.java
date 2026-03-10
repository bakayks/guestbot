package com.guestbot.core.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "room_photos")
@Getter @Setter @NoArgsConstructor
public class RoomPhoto extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private String url;

    private String minioKey;
    private Integer sortOrder = 0;
}
