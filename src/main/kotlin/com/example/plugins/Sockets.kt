package com.example.plugins

import com.example.leaders
import com.example.self
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer

suspend fun UDPListener(){
            println("Listening to UDP socket")
            val serverSocket =
                aSocket(SelectorManager(Dispatchers.IO)).udp().bind(InetSocketAddress(self.address, self.udpPort))
            while (true) {
                val input = serverSocket.incoming.receive()
                val messageTemplate = HashMap<String, String>()
                while (input.packet.isNotEmpty) {
                    var message = input.packet.readUTF8Line()
                    println("$message")
                    if ("$message".length == 4) message?.let { messageTemplate.put("port", it) }
                    else if ("$message" == "leader?") {
                        if (message != null) {
                            messageTemplate.put("message", message)
                        }
                    }
                    else{
                        if (message != null) {
                            messageTemplate.put("address", message)
                        }
                    }

                    if (messageTemplate.keys.size == 3){
                        var buf = BytePacketBuilder()
                        buf.writeText("${self.isLeader}")
                        serverSocket.outgoing.send(Datagram(buf.build(), InetSocketAddress(messageTemplate["address"]!!, messageTemplate["port"]!!.toInt())))
                    }
                }

        }
    println("Listening ended")
}

suspend fun TCPServer(){
        println("Listening to TCP socket")
        val server = aSocket(SelectorManager(Dispatchers.IO)).tcp().bind(InetSocketAddress(self.address, self.tcpPort))
        while (true) {
            val socket = server.accept()
            println("Accepted connection to ${socket.remoteAddress}")
                val receiveChannel = socket.openReadChannel()
                val sendChannel = socket.openWriteChannel(autoFlush = true)
                println("Received message ${receiveChannel.readUTF8Line()}")
                try {
                    var message = receiveChannel.readUTF8Line()
                        println("Received message $message")
                        if (message == "leader?") sendChannel.writeStringUtf8("${self.isLeader}")
                } catch (e: Throwable) {
                    socket.close()
                }
            socket.close()
    }

}

suspend fun UDPClient(address:InetSocketAddress){
        val socket = aSocket(SelectorManager(Dispatchers.IO)).udp().connect(address)
        val buffer = socket.openWriteChannel(true)
        var m = arrayListOf<String>(self.address, self.udpPort.toString(), "is leader?")
    for (c in m)
        buffer.writeStringUtf8(c)
        println("Sending message to $address")
        socket.close()
}

suspend fun TCPClient(id:Int, address: InetSocketAddress, message:String){
        val tcpSocket = aSocket(SelectorManager(Dispatchers.IO)).tcp().connect(address)
        println("Establishing TCP connection for ${address.hostname}")
        val read = tcpSocket.openReadChannel()
        val write = tcpSocket.openWriteChannel(autoFlush = true)
        write.writeStringUtf8(message)
        var answer = read.readUTF8Line()
        println(answer)
        if (answer == "false") leaders.set(id, false)
        if (true !in leaders.values) self.isLeader = true
        println("$self has become the leader")
}
