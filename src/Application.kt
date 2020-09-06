package com.itsamirrezah.youtubeapi

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(DefaultHeaders)
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
        }
    }

    routing {

        get("/playlist") {
            //get playlist id from url
            val id = call.request.queryParameters["id"]
            withContext(Dispatchers.Default) {
                //return the result as json to user
                call.respond(playlistItems(id))
            }
        }

        //TODO
        get("/search") {}
        get("/channel") {}
    }

}