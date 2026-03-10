package com.seungchan.realteeth.global.error.exception

import com.seungchan.realteeth.global.error.ErrorCode

class InvalidJobStateTransitionException : RealteethException(ErrorCode.INVALID_JOB_STATE_TRANSITION)