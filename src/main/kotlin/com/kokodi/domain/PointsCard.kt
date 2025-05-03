package com.kokodi.domain

import jakarta.persistence.*

@Entity
@DiscriminatorValue("POINTS")
open class PointsCard(
    override val name: String,
    override val value: Int
) : Card(name = name, value = value)