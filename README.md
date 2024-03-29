# data transfer protocol for lunar rover  
The file transfer protocol works on top of UDP. The reason for this is because TCP is usually very heavy   
on the network. And since we are sending and receiving between earth and moon, we want faster talk  
between the nodes. UDP allows us to do flow control on the application levelso we can design a flow   
that suits our needs.   

The protocol uses sequence numbers to keep track of packets.  
The protocol uses NACKs to signify a missing or dropped packet. It directly asks the missing sequence in   
the NACK. The sending node just resends the corresponding sequence for the NACK that it receives.  
The protocol does not implement a checksum itself butit relies on UDP’s checksum. If the packet is  
corrupted the packet is dropped before it reaches the application. Which eventually triggers a NACK.  
The receiver keeps sending NACKs for lost packets until all expected packets are received for the   
corresponding command.   

The protocol uses command and response IDs to communicate what is being asked or returned.   
The header is 10 bytes and contains the following in order:  
• rover id: 1 byte  
• command id: 2 bytes  
• response id: 2 bytes  
• seq number: 2 bytes  
• NACK number: 2 bytes  
• window size: 1 byte  

• [ data ]  
The command IDs are:  
• 100 = connect request  
• 201 = send most recent image  
• 420 = command end  

The response IDs are:  
• 100 = connect success  
• 111 = busy  
• 201 = sending most recent image ack  
• 211 = client ready to receive ack  
• 202 = image packet  
• 203 = final image packet  
• 220 = command end ack  

Image Data response packet containsthe following in the data field:  
• for response 201:  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;o size of each packet – 2 bytes  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;o number of packets – 2 bytes  

• for response 202:  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;o [image data]  

How to run:  
You can specify the maximum size for each packet   
Start rover first:  
```java rover [rover port] [size of each img data pkt in bytes] [image path]```  
  
Start client:  
```java client [client port] [rover port]```  


# RIPv2 Routing
Each rover can be treated as a router. There can be many rovers and they make up a network. 
The RIPv2 routing protocol helps to route the data to the desired rover.

Command line argument format for Main:

```java Main <MulticastIP> <MulticastPort> <RouterID>```

example: 
```java Main 230.230.230.230 63001 1```
