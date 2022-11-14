package com.example.plugins

import com.example.leaders
import com.example.self
import com.example.system
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import java.nio.ByteBuffer

var tcpSelectorManager = ActorSelectorManager(Dispatchers.IO)
var udpSelectorManager = ActorSelectorManager(Dispatchers.IO)

object UDPServer {
    @JvmStatic
     fun startServer() {
         runBlocking {
             println("Listening to UDP socket")
             val serverSocket =
                 aSocket(udpSelectorManager).udp().bind(InetSocketAddress(self.address, self.udpPort))
             while (true) {
                 val input = serverSocket.incoming.receive()
                 val messageTemplate = HashMap<String, String>()
                 while (input.packet.isNotEmpty) {
                     var message = input.packet.readUTF8Line()
                     println("$message")
                     if ("$message".length == 4) message?.let { messageTemplate.put("port", it) }

                 }
             }
             println("Listening ended")
         }
     }
}

object TCPServer {
    @JvmStatic
     fun startServer() {
         runBlocking {
             println("Listening to TCP socket")
             val server = aSocket(tcpSelectorManager).tcp().bind(InetSocketAddress(self.address, self.tcpPort))
             while (true) {
                 val socket = server.accept()
                 println("Accepted connection to ${socket.remoteAddress}")
                 val receiveChannel = socket.openReadChannel()
                 launch {
                         try {
                             while (true) {
                                 var message = receiveChannel.readUTF8Line()
                                 println("Received message $message")
                             }
                         } catch (e: Throwable) {
                             socket.close()
                         }
                     }
             }
         }

    }
}

object UDPClient {
    @JvmStatic
     fun sendMessage(address: InetSocketAddress) {
        runBlocking {
            val socket = aSocket(udpSelectorManager).udp().connect(address)
            val buffer = socket.openWriteChannel(true)
            var m = arrayListOf<String>(self.address, self.udpPort.toString(), "is leader?")
            for (c in m)
                buffer.writeStringUtf8(c)
            println("Sending message to $address")
            socket.close()
        }
    }
}
object TCPClient{
    @JvmStatic
 fun sendMessage(id:Int, address: InetSocketAddress, message:String){
        runBlocking {
            delay(system.tu.toLong())
            val tcpSocket = aSocket(tcpSelectorManager).tcp().connect(address)
            println("Establishing TCP connection for ${address.hostname}")
            val write = tcpSocket.openWriteChannel(autoFlush = true)
            write.writeStringUtf8(message+"\n")
        }
}
}
