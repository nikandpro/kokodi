package com.kokodi.domain

import jakarta.persistence.*

@Entity
@Table(name = "cards")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "card_type")
abstract class Card(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open val id: Long = 0,

    @Column(nullable = false)
    open val name: String,

    @Column(nullable = false)
    open val value: Int
) 