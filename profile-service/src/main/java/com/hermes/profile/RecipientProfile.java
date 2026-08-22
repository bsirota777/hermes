package com.hermes.profile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="recipient_profiles") @Getter @Setter
public class RecipientProfile extends ContactProfile {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id", nullable=false, unique=true) private Long userId;
}
