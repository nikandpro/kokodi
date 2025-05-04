package com.kokodi.domain

import jakarta.persistence.*
import github.nikandpro.com.kokodi.domain.ActionType

@Entity
@DiscriminatorValue("ACTION")
class ActionCard(
    name: String,
    @Column(name = "value", nullable = false)
    var value: Int,
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type")
    var actionType: ActionType
) : Card(name = name) {
    override fun toString(): String {
        return "ActionCard(id=$id, name='$name', value=$value, type=ACTION, actionType=$actionType)"
    }
}
