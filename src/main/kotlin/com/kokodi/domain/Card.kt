package com.kokodi.domain

import jakarta.persistence.*

@Entity
@Table(name = "cards")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "card_type", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("CARD")
abstract class Card(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long = 0,

    @Column(name = "name", nullable = false)
    open var name: String,

    @Column(name = "game_session_id")
    open var gameSessionId: Long? = null,

    @Column(name = "is_used", nullable = false)
    open var isUsed: Boolean = false
) {
    override fun toString(): String {
        return "Card(id=$id, name='$name', type=${javaClass.simpleName}, isUsed=$isUsed)"
    }
} 