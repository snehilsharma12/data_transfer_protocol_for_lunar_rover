# RIPv2 Routing Protocol  
Implements the protocol. The working of this can be tested on a multicast docker environment.  
Implements Split Horizon and Route Poisoning to deal with infinite costs and routing loops.  
Uses Bellman-Ford algorithm to calculate shortest paths to other routers.  
The implementation is multi-threaded.  

Command line argument format for Main:  

$> MulticastIP MulticastPort RouterID  

example: 230.230.230.230 63001 1
