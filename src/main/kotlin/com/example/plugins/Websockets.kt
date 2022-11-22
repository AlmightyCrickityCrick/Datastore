package com.example.plugins

import com.example.*
import com.example.Connection
import io.ktor.network.sockets.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import java.time.Duration
import java.util.Collections

fun Application.configureWebSockets(){
    install(WebSockets){
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        webSocket("/chat"){
            println("New User")
            val thisConnection = Connection(this)
            connections.plusAssign(thisConnection)
            try {
                send("Connection established. Currently ${connections.count()} users here")
                for (frame in incoming){
                    frame as? Frame.Text?:continue
                    val receivedText = frame.readText()
                    val d = Data(count++, "[${thisConnection.name}]:$receivedText", System.currentTimeMillis())
                    val loc = getContainers()
                    dataLocation.put(d.id, loc)
                    if(system.self.id in loc) data.put(d.id, d)
                    for (p in system.peers) if(p.id in loc &&p.isUp)TCPClient(p.id, InetSocketAddress(p.address, p.tcpPort), Message(MessageType.dataSend, Json.encodeToString(Data.serializer(), d)))
                    connections.forEach{
                        it.session.send(d.content)
                    }
                }
            } catch (e:Exception) {

            }finally {
                connections.minusAssign(thisConnection)
            }


        }
    }
}