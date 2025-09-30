package com.waalterGar.projects.ecommerce.entity;

import com.waalterGar.projects.ecommerce.utils.CountryCode;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "customers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_customer_email", columnNames = "email")
        },
        indexes = {
                @Index(name = "idx_customer_email", columnList = "email")
        }
)
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable=false, unique=true, length=36)
    private String externalId;

    @NotBlank
    @Size(max = 80)
    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @NotBlank
    @Size(max = 80)
    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @NotBlank
    @Email
    @Size(max = 180)
    @Column(nullable = false, length = 180)
    private String email;

    @Size(max = 30)
    @Column(length = 30)
    private String phoneNumber;

    @Size(max = 120)
    @Column(length = 120)
    private String address;

    @Size(max = 80)
    @Column(length = 80)
    private String city;

    @Size(max = 80)
    @Column(length = 80)
    private String state;

    @Size(max = 20)
    @Column(length = 20)
    private String zipCode;

    @Enumerated(EnumType.STRING)
    @Column(length = 2, nullable = false)
    private CountryCode countryCode;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        normalizeEmail();
        if (isActive == null) {
            isActive = true;
        }
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        normalizeEmail();
        updatedAt = LocalDateTime.now();
    }

    private void normalizeEmail() {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }
}
