package com.example

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.example.plugins.*
import io.ktor.client.*
import io.ktor.network.sockets.*
import com.example.Connection
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Collections
import kotlin.coroutines.CoroutineContext

lateinit var system : Instance
var leaderStatus=HashMap<Int, Boolean>()
var currentLeader = 0

var data = HashMap<Int, Data>()
var dataLocation = HashMap<Int, ArrayList<Int>>() // idData:listServers
var faultToleranceSize = 0
var count = 0
var client = HttpClient()
var connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())


fun main() {
    system = Json.decodeFromString(Instance.serializer(), File("config/config.json").inputStream().readBytes().toString(Charsets.UTF_8))
    for (p in system.peers) leaderStatus[p.id]= true
    faultToleranceSize = Math.floor((((system.peers.size + 1)/2).toDouble())).toInt() + 1
    println(system.self)
    CoroutineScope(Dispatchers.Default).launch {  UDPListener()}
    CoroutineScope(Dispatchers.Default).launch { TCPServer() }

    system.self.isUp = true
    notifyUp()
    openHttp()
    CoroutineScope(Dispatchers.Default).launch { synchronizeData() }
    checkLeader()
    if (system.self.isLeader) {
        count = dataLocation.size
        println("!!!!!!!!!!\n\n${system.self} is the leader\n\n!!!!!!!!!!\n" +
                "\n")
        continueSynchronizationCheck()
    }
}

fun openHttp(){
    embeddedServer(Netty, port = system.self.httpPort) {
        configureAdministration()
        configureRouting()
        configureWebSockets()


    }.start(wait = false)
}

fun notifyUp(){
    println("Notifying that the server is up")
    runBlocking {
        delay(3000)
    for (p in system.peers) launch{ UDPClient(InetSocketAddress(p.address, p.udpPort), Message(MessageType.serverUp, "${system.self.id}")) }
    }
}
 fun checkLeader(){
     runBlocking {
         while (!system.self.isLeader) {
             delay(system.tu.toLong())
             println("Sending a message to peers about leaders")
            for (p in system.peers) if(p.isUp)launch{TCPClient(p.id, InetSocketAddress(p.address, p.tcpPort), Message(MessageType.leaderRequest, "${system.self.id}"))}
             //for (p in system.peers) launch{ UDPClient(InetSocketAddress(p.address, p.udpPort)) }

         }
     }
}

fun synchronizeDataLocation(){
    println("Synchronizing Location data")
    runBlocking {
        for (p in system.peers) if(p.isUp)launch{ UDPClient(InetSocketAddress(p.address, p.udpPort), Message(MessageType.locationSync, Json.encodeToString(
            SyncronizeLocationrequest.serializer(), SyncronizeLocationrequest(dataLocation)
        ))
        ) }
    }
}

fun synchronizeData(){
    runBlocking {
        while (true){
            delay(2000)
            for (p in system.peers) if (p.isUp){
                println("Compiling data for sync between ${system.self.id} and ${p.id}")
                var dlist = HashMap<Int, Data>()
                for (d in data){
                    if (dataLocation[d.key]?.contains(p.id) == true) dlist.put(d.key, d.value)
                }
                launch{TCPClient(p.id, InetSocketAddress(p.address, p.tcpPort), Message(MessageType.dataSync, Json.encodeToString(SyncDataList.serializer(), SyncDataList(
                    system.self.id, dlist)
                )))}
            }
        }
    }
}

fun continueSynchronizationCheck(){
    runBlocking {
        while (true) {
            delay(5000)
            for (p in system.peers) if(!p.isUp){
                for (i in dataLocation.keys){
                    if (dataLocation[i]?.contains(p.id) == true) dataLocation[i]?.remove(p.id)
                }
            }
            launch{
                synchronizeDataLocation()
            }
        }
    }
}
