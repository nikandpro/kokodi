package com.kokodi.domain

import github.nikandpro.com.kokodi.domain.ActionType
import jakarta.persistence.*

@Entity
@DiscriminatorValue("ACTION")
open class ActionCard(
    override val name: String,
    override val value: Int,
    @Enumerated(EnumType.STRING)
    open val actionType: ActionType
) : Card(name = name, value = value)
