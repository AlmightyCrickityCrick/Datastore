package com.example.plugins

import com.example.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.network.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.nio.channels.UnresolvedAddressException

fun UDPListener(){
    runBlocking {
        println("Listening to UDP socket")
        val serverSocket =
            aSocket(SelectorManager(Dispatchers.IO)).udp().bind(InetSocketAddress(system.self.address, system.self.udpPort))
        while (true) {
            val input = serverSocket.incoming.receive()
            while (input.packet.isNotEmpty) {
                var message = input.packet.readUTF8Line()
                println("$message")
                var m = message?.let { Json.decodeFromString(Message.serializer(), it) }
                if (m != null) {
                    println("UDP request received $m from ${input.address}")
                    when(m.type){
                        MessageType.serverUp ->{
                            println("Received message that ${m.content} is up")
                            for (p in system.peers) if (p.id == m.content.toInt()) p.isUp = true
                        }
                        MessageType.serverDownAlertPassive -> {
                            println("Received message that ${m.content} is down")
                            for (p in system.peers) if (p.id == m.content.toInt()) p.isUp = false
                            leaderStatus[m.content.toInt()] =  false
                        }

                        MessageType.serverDownAlertActive -> {
                            println("Received message that ${m.content} is down")
                            var toAdd = ArrayList<Int>()
                            for (p in system.peers) if (p.id == m.content.toInt()) p.isUp = false
                            for (obj in dataLocation.keys) if (dataLocation[obj]?.contains(m.content.toInt()) == true && dataLocation[obj]?.contains(system.self.id)==false){
                                toAdd.add(obj)
                                data.put(obj, Data(0, "", 0))
                            }
                            var lead:Node? = null
                            leaderStatus[m.content.toInt()] =  false
                            checkLeader()
                            for (p in system.peers) if (p.isLeader) {lead = p
                            break}
                            TCPClient(currentLeader, InetSocketAddress(lead!!.address, lead.tcpPort), Message(MessageType.updateLocations, Json.encodeToString(UpdateLocationRequest.serializer(), UpdateLocationRequest(system.self.id, toAdd))) )
                        }
                        MessageType.locationSync -> {
                            println("Received Location Synchronization message")
                            var tmp =Json.decodeFromString(SyncronizeLocationrequest.serializer(), m.content)
                            dataLocation = tmp.dataLocation
                        }
                        else ->{}
                    }
                    }
            }

        }
    }
    println("Listening ended")
}

 fun TCPServer(){
    runBlocking {
        println("Listening to TCP socket")
        val server = aSocket(SelectorManager(Dispatchers.IO)).tcp().bind(InetSocketAddress(system.self.address, system.self.tcpPort))
        while (true) {
            val socket = server.accept()
            println("Accepted connection to ${socket.remoteAddress}")
            val receiveChannel = socket.openReadChannel()
            launch {
                try {
                while (true) {
                        var message = receiveChannel.readUTF8Line()
                        var m = message?.let { Json.decodeFromString(Message.serializer(), it) }
                    if (m != null) {
                        println("TCP request received $m from ${socket.remoteAddress}")
                        when(m.type){
                            MessageType.leaderRequest -> {
                                var peer :Node?= null
                                    for (p in system.peers) if (p.id == m.content.toInt()) {
                                        peer = p
                                        break
                                    }
                               launch{
                                   if (peer != null) {
                                       TCPClient(peer.id, InetSocketAddress(peer.address, peer.tcpPort) ,Message(MessageType.leaderResponse, Json.encodeToString(LeaderMessage.serializer(), LeaderMessage(
                                           system.self.id, system.self.isLeader))))
                                   }
                               }
                            }
                            MessageType.leaderResponse ->{
                                var tmp = Json.decodeFromString(LeaderMessage.serializer(),m.content)
                                leaderStatus[tmp.id] = tmp.isLeader
                                if(tmp.isLeader) currentLeader = tmp.id
                                if (true !in leaderStatus.values) {system.self.isLeader = true
                                currentLeader = system.self.id}
                            }
                            MessageType.dataSend -> {
                                var tmp = Json.decodeFromString(Data.serializer(), m.content)
                                println("Received $tmp")
                                data.put(tmp.id, tmp)
                            }

                            MessageType.updateLocations -> {
                                println("Received request to add a server to dataLocation Map")
                                var tmp = Json.decodeFromString(UpdateLocationRequest.serializer(), m.content)
                                for (i in tmp.datas) dataLocation[i]?.add(tmp.id)
                                synchronizeDataLocation()
                            }

                            else -> {}
                        }
                    }
                    }
                } catch (e: Throwable) {
                        socket.close()

                }
            }
        }
    }

}

suspend fun UDPClient(address:InetSocketAddress, message: Message){
        val socket = aSocket(SelectorManager(Dispatchers.IO)).udp().connect(address)
        val buffer = socket.openWriteChannel(true)
        buffer.writeStringUtf8(Json.encodeToString(Message.serializer(), message) + "\n")
        println("Sending message to $address")
}

suspend fun TCPClient(id:Int, address: InetSocketAddress, message:Message){
    try {
    val tcpSocket = aSocket(SelectorManager(Dispatchers.IO)).tcp().connect(address)
        println("Establishing TCP connection for ${address.hostname}")
        val write = tcpSocket.openWriteChannel(autoFlush = true)
        write.writeStringUtf8(Json.encodeToString(Message.serializer(), message) + "\n")
    } catch (err:UnresolvedAddressException) {
        runBlocking {
            launch {
                var tmp = 0
                UDPClient(
                    InetSocketAddress(system.self.address, system.self.udpPort),
                    Message(MessageType.serverDownAlertActive, "$id")
                )
                for (p in system.peers)
                    if (tmp < faultToleranceSize - 1) {
                        tmp++
                        UDPClient(
                            InetSocketAddress(p.address, p.udpPort),
                            Message(MessageType.serverDownAlertActive, "$id")
                        )
                    } else UDPClient(
                        InetSocketAddress(p.address, p.udpPort),
                        Message(MessageType.serverDownAlertPassive, "$id")
                    )
            }
        }
    }
}
