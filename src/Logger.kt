package com.itsamirrezah.youtubeapi

import org.slf4j.Logger
import org.slf4j.LoggerFactory


val log: Logger = LoggerFactory.getLogger("YT-API")

fun log(message: String) {
    log.info(message)
}