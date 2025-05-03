package com.kokodi.domain

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @NotBlank
    @Size(min = 3, max = 50)
    @Column(unique = true)
    val username: String,

    @NotBlank
    @Size(min = 6)
    val password: String,

    @NotBlank
    @Size(min = 2, max = 50)
    val name: String,

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL])
    val gameSessions: MutableList<GameSessionPlayer> = mutableListOf()
) 