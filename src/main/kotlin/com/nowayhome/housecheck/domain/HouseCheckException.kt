package com.nowayhome.housecheck.domain

import com.nowayhome.housecheck.application.HouseCheckErrorCode

class HouseCheckException(
    val errorCode: HouseCheckErrorCode,
    override val message: String = errorCode.defaultMessage,
) : RuntimeException(message)
