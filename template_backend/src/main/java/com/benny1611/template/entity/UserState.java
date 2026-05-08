package com.benny1611.template.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "user_states")
@Data
public class UserState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;
}
