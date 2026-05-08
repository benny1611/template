package com.benny1611.template.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "oauth_providers")
@Data
public class OauthProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;
}
