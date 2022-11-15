package com.example.plugins

import com.example.*
import io.ktor.client.request.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.network.sockets.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import kotlinx.coroutines.delay
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json


fun Application.configureRouting() {
    routing {
        get("get/{id}") {
            var dataId = call.parameters["id"]?.toInt()
            if (dataId == null || dataId !in dataLocation.keys) {
                call.respond("Data not found")
            } else {
                var tmp:Data? = null
                if(dataId !in data.keys){
                    var src :Node? = null
                    for (p in system.peers) if (dataLocation[dataId]?.contains(p.id) == true) {
                        src = p
                        break}
                    while(dataId !in data.keys) {
                        if (src != null) {
                            TCPClient(src.id, InetSocketAddress(src.address, src.tcpPort), Message(MessageType.dataGet, "$dataId"))
                        }
                        delay(300)
                    }
                }
                tmp = data[dataId]
                if(dataLocation[dataId]?.contains(system.self.id) != true) data.remove(dataId)
                if (tmp != null) {
                    call.respond(Json.encodeToString(String.serializer(), tmp.content))
                }
            }
        }

        post("/post") {
            var d = call.receive<String>()
            var tmp = Data(count, d, System.currentTimeMillis())
            call.respondText(count.toString())
            var i = 0
            var toAdd =  ArrayList<Int>()
            while(i < faultToleranceSize){
                var t = (1..3).random()
                if(t !in toAdd){
                    i++
                    toAdd.add(t)}
            }
            dataLocation.put(count, toAdd)
            if(system.self.id in toAdd){
                data[count] = tmp
            }
            for (p in system.peers) {
                if(p.id in toAdd)TCPClient(p.id, InetSocketAddress(p.address, p.tcpPort), Message(MessageType.dataSend, Json.encodeToString(Data.serializer(), tmp)))
            }
            count++
            synchronizeDataLocation()
        }

        post("/post/{id}") {
            var dataId = call.parameters["id"]?.toInt()
            var d = call.receive<String>()
            if (dataId == null || dataId !in dataLocation.keys) {
                call.respond("Data not found")
            } else {
                var tmp = Data(dataId, d, System.currentTimeMillis())
                call.respond(HttpStatusCode.Created)
                if (dataLocation[dataId]?.contains(system.self.id) == true) data.put(dataId, tmp)
                for (p in system.peers) {
                    if(dataLocation[dataId]?.contains(p.id) == true)TCPClient(p.id, InetSocketAddress(p.address, p.tcpPort), Message(MessageType.dataSend, Json.encodeToString(Data.serializer(), tmp)))
                }
            }
        }

        delete("/delete/{id}"){
            var dataId = call.parameters["id"]?.toInt()
            if (dataId == null || dataId !in data.keys) {
                call.respond("Data not found")
            } else {
                if(dataLocation[dataId]?.contains(system.self.id) == true)data.remove(dataId)
                call.respond(HttpStatusCode.Gone)
                for (p in system.peers) {
                    if(dataLocation[dataId]?.contains(p.id) == true)TCPClient(p.id, InetSocketAddress(p.address, p.tcpPort), Message(MessageType.dataDelete, "$dataId"))
                }
                dataLocation.remove(dataId)
                synchronizeDataLocation()
            }
        }
    }
}