package com.guestbot.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Boolean> amenities;

    private String checkInTime;
    private String checkOutTime;
    private Integer minAge;
    private Boolean petsAllowed;
    private Boolean childrenAllowed;

    @Column(columnDefinition = "TEXT")
    private String cancellationPolicy;

    private String bankName;
    private String bankAccount;
    private String taxId;
    private String bankRecipient;

    private Boolean botActive = false;

    @Column(columnDefinition = "TEXT")
    private String welcomeMessage;

    @Column(columnDefinition = "TEXT")
    private String offHoursMessage;

    private String workingHoursStart;
    private String workingHoursEnd;

    @JsonIgnore
    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Room> rooms = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HotelPhoto> photos = new ArrayList<>();

    @Column(nullable = false)
    private boolean active = true;
}
