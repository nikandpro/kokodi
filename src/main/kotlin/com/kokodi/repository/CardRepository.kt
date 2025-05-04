package com.kokodi.repository

import com.kokodi.domain.Card
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface CardRepository : JpaRepository<Card, Long> {
    @Modifying
    @Query("UPDATE Card c SET c.isUsed = true WHERE c.id = :cardId")
    fun markCardAsUsed(cardId: Long)
}