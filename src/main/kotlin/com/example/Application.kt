package com.example

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.example.plugins.*
import io.ktor.client.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.coroutines.CoroutineContext

lateinit var system : Instance
lateinit var self :Node
var TU: Int = 0
var leaders=HashMap<Int, Boolean>()

var data = HashMap<Int, String>()
var count = 0
var client = HttpClient()
fun main() {
    system = Json.decodeFromString(Instance.serializer(), File("config/config.json").inputStream().readBytes().toString(Charsets.UTF_8))
    self = system.self
    TU = system.tu
    for (p in system.peers) leaders[p.id]= true
    println(self)
    CoroutineScope(Dispatchers.Default).launch {  UDPListener()}
    CoroutineScope(Dispatchers.Default).launch { TCPServer() }
    checkLeader()
    if (self.isLeader) println("$self is the leader")
    openHttp()
}

fun openHttp(){
    embeddedServer(Netty, port = self.httpPort) {
        configureAdministration()
        configureRouting()


    }.start(wait = true)
}

 fun checkLeader(){
     runBlocking {
         while (!self.isLeader) {
             delay(system.tu.toLong())
             println("Sending a message to peers")
            for (p in system.peers) launch{TCPClient(p.id, InetSocketAddress(p.address, p.tcpPort), "leader?")}
             //for (p in system.peers) launch{ UDPClient(InetSocketAddress(p.address, p.udpPort)) }

         }
     }
}
