package com.example.plugins

import com.example.client
import com.example.count
import com.example.data
import com.example.system
import io.ktor.client.request.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


fun Application.configureRouting() {
    routing {
        get("get/{id}") {
            var dataId = call.parameters["id"]?.toInt()
            if (dataId == null || dataId !in data.keys) {
                call.respond("Data not found")
            } else {
                var obj: String = data[dataId].toString()
                call.respond(Json.encodeToString(String.serializer(), obj))
            }
        }

        post("/post") {
            var d = call.receive<String>()
            data[count] = d
            call.respondText(count.toString())
            for (p in system.peers) {
                println("http://${p.address}:${p.httpPort}/post/$count")
                val resp = client.post("http://${p.address}:${p.httpPort}/post/$count") {
                    setBody(d)
                }
            }
            count++
        }

        post("/post/{id}") {
            var dataId = call.parameters["id"]?.toInt()
            var d = call.receive<String>()
            if (dataId == null) {
                call.respond("Data not found")
            } else {
                data.put(dataId, d)
                call.respond(HttpStatusCode.Created)
            }
        }

        delete("/delete/{id}"){
            var dataId = call.parameters["id"]?.toInt()
            if (dataId == null || dataId !in data.keys) {
                call.respond("Data not found")
            } else {
                data.remove(dataId)
                call.respond(HttpStatusCode.Gone)
            }
        }
    }
}
