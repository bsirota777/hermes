package com.hermes.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sender_profiles")
@Getter
@Setter
public class SenderProfile extends ContactProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // shared PK with User

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
}