package com.example

import kotlinx.serialization.Serializable

@Serializable
data class Instance(var self:Node, var peers:ArrayList<Node>, var tu:Int)
@Serializable
data class Node(var id: Int, var address: String, var httpPort: Int, val tcpPort: Int, var udpPort: Int, var isLeader:Boolean, var isUp:Boolean)

@Serializable
data class Message(var type: MessageType, var content:String)

@Serializable
data class  SyncDataList(var node_id:Int, var data_list:HashMap<Int, Data>)
@Serializable
data class Data(var id:Int, var content:String, var modificationTime : Long)
@Serializable
enum class MessageType{
    leaderRequest, //Asks if is leader
    leaderResponse, //Sends self isLeader status
    dataSend, //Sends data over tcp from http server or to a server in case of a serverDownAlert
    dataSendRequest, //Asks server to send a specific data to originator based on id
    dataSync, //Sends all data from server for synchronization purposes
    serverDownAlertActive, //Sends notification that one of the servers isn't responding. Forces the servers to pick up its data
    serverDownAlertPassive, //Sends notification that a server is down
    serverUp, //Sends notification that the server is up
    locationSync, //Syncronizes map with data location. Sent through UDP by leader
    updateLocations, //Sent to leader to ask to add the server to the dataLocations of those datas
    dataDelete, //Sent to the servers when data is deleted with id of data to delete
    dataGet, //placeHolder messageType that gets data from another server to  leader
}

@Serializable
data class LeaderMessage(val id: Int, val isLeader: Boolean)

@Serializable
data class SyncronizeLocationrequest(var dataLocation : HashMap<Int, ArrayList<Int>>)
@Serializable
data class UpdateLocationRequest(val id: Int, val datas: ArrayList<Int>)