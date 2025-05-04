package com.kokodi.domain

import jakarta.persistence.*

@Entity
@DiscriminatorValue("POINTS")
class PointsCard(
    name: String,
    @Column(name = "value", nullable = false)
    var value: Int
) : Card(name = name) {
    override fun toString(): String {
        return "PointsCard(id=$id, name='$name', value=$value, type=POINTS)"
    }
}