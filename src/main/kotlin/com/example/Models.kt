package com.example

import kotlinx.serialization.Serializable

@Serializable
data class Instance(var self:Node, var peers:ArrayList<Node>, var tu:Int)
@Serializable
data class Node(var id: Int, var address: String, var httpPort: Int, val tcpPort: Int, var udpPort: Int, var isLeader:Boolean)