package com.guestbot.core.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "hotels")
@Getter
@Setter
@NoArgsConstructor
public class Hotel extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String address;
    private String city;
    private String phone;
    private String email;
    private String website;

    private Double latitude;
    private Double longitude;

    // JSONB — гибкий список удобств
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Boolean> amenities;

    // Правила заселения
    private String checkInTime;
    private String checkOutTime;
    private Integer minAge;
    private Boolean petsAllowed;
    private Boolean childrenAllowed;

    @Column(columnDefinition = "TEXT")
    private String cancellationPolicy;

    // Банковские реквизиты для выплат
    private String bankName;
    private String bankAccount;
    private String taxId;
    private String bankRecipient;

    // Telegram бот токен для этой гостиницы
    private String telegramBotToken;
    private Boolean botActive = false;

    // Приветственное сообщение
    @Column(columnDefinition = "TEXT")
    private String welcomeMessage;

    @Column(columnDefinition = "TEXT")
    private String offHoursMessage;

    private String workingHoursStart;
    private String workingHoursEnd;

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Room> rooms = new ArrayList<>();

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HotelPhoto> photos = new ArrayList<>();

    @Column(nullable = false)
    private boolean active = true;
}
