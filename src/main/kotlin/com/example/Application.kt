package com.example

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.example.plugins.*
import kotlinx.serialization.json.Json
import java.io.File

lateinit var system : Instance
lateinit var self :Node
var TU: Int = 0

fun main() {
    system = Json.decodeFromString(Instance.serializer(), File("config/config.json").inputStream().readBytes().toString(Charsets.UTF_8))
    self = system.self
    TU = system.tu
    println(self)
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSockets()
    configureAdministration()
    configureRouting()
}
