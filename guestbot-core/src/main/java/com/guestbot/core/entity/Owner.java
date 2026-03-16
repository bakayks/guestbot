package com.guestbot.core.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "owners")
@Getter
@Setter
@NoArgsConstructor
public class Owner extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    private String telegramChatId;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean emailVerified = false;
}
