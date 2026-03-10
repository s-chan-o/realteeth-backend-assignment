package com.seungchan.realteeth.global.error.exception

import com.seungchan.realteeth.global.error.ErrorCode

class JobNotFoundException : RealteethException(ErrorCode.JOB_NOT_FOUND)