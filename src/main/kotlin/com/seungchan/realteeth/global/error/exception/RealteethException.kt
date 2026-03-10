package com.seungchan.realteeth.global.error.exception

import com.seungchan.realteeth.global.error.ErrorCode

open class RealteethException(val errorCode: ErrorCode) : RuntimeException(errorCode.message)