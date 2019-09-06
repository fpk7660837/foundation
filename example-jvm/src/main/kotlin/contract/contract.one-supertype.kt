package com.example.contract

import net.ntworld.foundation.Faked
import net.ntworld.foundation.FakedData
import net.ntworld.foundation.Handler
import net.ntworld.foundation.cqrs.Command
import net.ntworld.foundation.cqrs.CommandHandler


interface OneSupertypeContractParent {
    @get:Faked(type = FakedData.Zelda.character)
    val zelda: String

    @Faked(type = FakedData.Name.firstName)
    val firstName: String

    @Faked(type = FakedData.Name.lastName)
    val lastName: String
}

interface OneSupertypeContract : OneSupertypeContractParent, Command {
    @EmailFaked
    val email: String

    companion object
}

@Handler
class OneSupertypeContractHandler : CommandHandler<OneSupertypeContract> {
    override fun handle(command: OneSupertypeContract) {
    }
}
