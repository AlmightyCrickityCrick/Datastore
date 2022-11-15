package com.example.plugins

import com.example.*
import io.ktor.client.request.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.network.sockets.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json


fun Application.configureRouting() {
    routing {
        get("get/{id}") {
            var dataId = call.parameters["id"]?.toInt()
            if (dataId == null || dataId !in data.keys) {
                call.respond("Data not found")
            } else {
                var obj: String = data[dataId]!!.content
                call.respond(Json.encodeToString(String.serializer(), obj))
            }
        }

        post("/post") {
            var d = call.receive<String>()
            var tmp = Data(count, d, System.currentTimeMillis())
            data[count] = tmp
            call.respondText(count.toString())
            for (p in system.peers) {
                TCPClient(p.id, InetSocketAddress(p.address, p.udpPort), Message(MessageType.dataSend, Json.encodeToString(Data.serializer(), tmp)))
            }
            count++
        }

        post("/post/{id}") {
            var dataId = call.parameters["id"]?.toInt()
            var d = call.receive<String>()
            if (dataId == null) {
                call.respond("Data not found")
            } else {
                data.put(dataId, Data(dataId, d, System.currentTimeMillis()))
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
