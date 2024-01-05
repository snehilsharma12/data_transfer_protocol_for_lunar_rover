/**
 * A routing table object
 */

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;

public class rtable implements Serializable{

    void writeObject(ObjectOutputStream out) throws IOException{

        out.defaultWriteObject();  
    }

    void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException{

        in.defaultReadObject();  
    }



    ArrayList<route> table;

    rtable(){

        table = new ArrayList<route>();
    }
    

    void add(int id, InetAddress router,InetAddress mask, InetAddress next_hop, int cost){

        table.add(new route(id, router,  mask, next_hop, cost));
    }

    route get(int i){
        return table.get(i);
    }

    ArrayList<route> get_ArrayList(){
        return table;

    }

    void remove(int i){
        table.remove(i);
    }

    boolean isEmpty(){

        return table.isEmpty();
    }

    int size(){
        return table.size();
    }

    boolean has(InetAddress router){
        for(int i=0; i<table.size(); i++){

            if (router.equals( (table.get(i)).router )){

                return true;
            }

            else{
                continue;
            }
        }

        return false;
    }

    route getRoute(InetAddress router){

        if (this.has(router)){

            for(int i=0; i<this.size(); i++){

                if (router.equals( (this.get(i)).router )){
    
                    return this.get(i);
                }
    
                else{
                    continue;
                }
            }
        }   

        return null;
    
    }

    int getIndex(InetAddress router){

        if (this.has(router)){

            for(int i=0; i<this.size(); i++){

                if (router.equals( (this.get(i)).router )){
    
                    return i;
                }
    
                else{
                    continue;
                }
            }
        }   

        return -1;
    }

    void add_self(int my_id, InetAddress my_ip, InetAddress my_mask){
        
        this.add(my_id, my_ip,my_mask, my_ip, 0);    
    
    }

}
