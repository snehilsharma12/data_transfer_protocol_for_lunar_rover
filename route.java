/**
 * A route object
 */

import java.net.InetAddress;

public class route {

        InetAddress router;
        InetAddress next_hop;
        int cost;
        int id;
        InetAddress mask;

        route(){
            
        }

    
        route(int id, InetAddress router, InetAddress mask, InetAddress next_hop, int cost){
            
            this.id = id;
            this.router = router;
            this.next_hop = next_hop;
            this.cost = cost;
            this.mask = mask;

        }
    
}
