/**
 * Rover that receives commands and 
 * sends an image to the client
 * 
 * @author: Snehil Sharma
 * 
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;


public class rover extends Thread{


    static int rover_id = 1;
    static int window = 10;

    static DatagramSocket my_skt;
    static int my_port;
    static int client_port;
    static InetAddress client_addr;
    static String image_path;
    static int data_size;
    static int client_seq_num = 0;
    static int pkt_counter = 0;
    static byte[] img;
    static int image_seq_end = 0;

    static Boolean client_confirm_flag = false;

    //hashmapof packets with <sequence numeber, associated packet>
    static HashMap<String, DatagramPacket> packets = new HashMap<String, DatagramPacket>();

    static ArrayList<byte[]> img_data = new ArrayList<byte[]>();

    /**
     * makes image into a byte[]
     * @param image_path
     * @return byte[]
     */
    public byte[] get_image(String image_path){
      
        try {


            File file = new File(image_path);
            byte[] bytes = new byte[(int) file.length()];
        
            try(FileInputStream fis = new FileInputStream(file)){
                fis.read(bytes);
            }

            return bytes;

        } 
        
        catch (Exception e) {
            
            e.printStackTrace();
            System.out.println(e);
            byte[] err = {-1};
            return err;
        }
        
    }


    // Converts the img array into smaller byte arrays 
    public void make_img_data(int size_of_data, byte[] img){

        byte[] segment;

        for(int i=0; i<img.length; i+=data_size){

            if( (img.length - i) < data_size){

                segment = new byte[(img.length)-i];

                System.arraycopy(img, i, segment, 0, segment.length);
            }

            else{
                segment = new byte[data_size];

                System.arraycopy(img, i, segment, 0, data_size);
            }

  
            img_data.add(segment);
            pkt_counter += 1;
   
        }

        //System.out.println("Img data arraylist made");

    }

    //makes a header byte array for the  given header parameters
    public byte[] get_header(int command_id, int resp_id, int seq, int nack){

        byte[] header_arr = new byte[10];

        header_arr[0] = (byte) rover_id;

        header_arr[1] = (byte) ((command_id >> 8) & 0xFF);
        header_arr[2] = (byte) (command_id & 0xFF);

        header_arr[3] = (byte) ((resp_id >> 8) & 0xFF);
        header_arr[4] = (byte) (resp_id & 0xFF);

        header_arr[5] = (byte) ((seq >> 8) & 0xFF);
        header_arr[6] = (byte) (seq & 0xFF);

        header_arr[7] = (byte) ((nack >> 8) & 0xFF);
        header_arr[8] = (byte) (nack & 0xFF);

        header_arr[9] = (byte) window;

        return header_arr;

    }


    // takes the small img arrays and puts them in packets
    public void make_img_pkts(int seq_start, int number_of_packets){

        int seq_end = seq_start + pkt_counter - 1;

        image_seq_end = seq_end;

        byte[] header;

        for(int i=0; i<pkt_counter; i++ ){

            //make the header
            header = get_header(201, 202, seq_start + i, 0);

            byte[] temp_data = img_data.get(i);

            byte[] temp_pkt = new byte[header.length + temp_data.length];


            ByteBuffer a = ByteBuffer.wrap(temp_pkt);

            a.put(header);
            a.put(temp_data);

            DatagramPacket pkt = new DatagramPacket(temp_pkt, temp_pkt.length, client_addr, client_port);

            packets.put( String.valueOf(i+seq_start) , pkt);


        }

        //System.out.println(packets);

    }


    //makes a response packet and puts it in the packets hashmap with seq num
    public void make_response(int command_id, int resp_id, int seq, int nack, byte[] data){

        byte[] header = get_header(command_id, resp_id, seq, nack);

        byte[] packet = new byte[header.length + data.length];

        System.arraycopy(header, 0, packet, 0, header.length);
        System.arraycopy(data, 0, packet, header.length, data.length);

        DatagramPacket pkt =  new DatagramPacket(packet, packet.length, client_addr, client_port);

        packets.put(String.valueOf(seq), pkt);

    }


    //sends the response packet with the corresponding seq num
    public void send_response(int seq){

        try {

            DatagramPacket pkt = packets.get(String.valueOf(seq));

            my_skt.send(pkt);


        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }

    }


    //converts two bytes to int
    public int get_int(Byte b1, Byte b2){

        int num = ((b1 & 0xff) << 8) | (b2 & 0xff);

        return num;
    }

    //sends the img data packets
    public void send_image_helper(int seq_start){
        try {

            System.out.println("Sending Image Packets!");

            

            for(int i=0; i<pkt_counter; i++){


                DatagramPacket current_pkt = packets.get(String.valueOf(seq_start+i));

                my_skt.send(current_pkt);

            }

            //System.out.println("SEND SEQ NUM: " + image_seq_end);

            DatagramPacket last_pkt = packets.get(String.valueOf(image_seq_end));

            my_skt.send(last_pkt);

            System.out.println("All image packets sent!");
            
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }

        

    }

    /**
     * sets up img data packets and 
     * sends the specification packet
     * @param packet_size
     * @param seq_ack
     */
    public void send_image(int packet_size, int seq_ack){

        int seq_start = seq_ack + 1;
        img = get_image(image_path);
        
        //System.out.println("IMG[] SIZE: " + img.length);
        
        make_img_data(packet_size, img);
        int number_of_packets = pkt_counter;
        System.out.println("No. of img pkts: " + number_of_packets);
        make_img_pkts(seq_start, number_of_packets);

        //System.out.println("IMG Packets made!");

        byte[] ack_data = new byte[4];

        ack_data[0] =(byte) ((packet_size >> 8) & 0xFF);
        ack_data[1] =(byte) (packet_size & 0xFF);

        ack_data[2] =(byte) ((number_of_packets >> 8) & 0xFF);
        ack_data[3] =(byte) (number_of_packets & 0xFF);

        try {

            make_response(201, 201, seq_ack, 0, ack_data);

            DatagramPacket ack_pkt = packets.get(String.valueOf(seq_ack));

            //SEND IMG SPECS
            my_skt.send(ack_pkt);

            //start sending img data packets
            send_image_helper(seq_start);


        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
        
    }




    public void run(){

        while(true){

            byte[] pkt;
            synchronized(image_path){

                byte[] com_received = new byte[256];

                DatagramPacket com_pkt = new DatagramPacket(com_received, com_received.length);

                try {

                    my_skt.receive(com_pkt);

                    client_port = com_pkt.getPort();
                    client_addr = com_pkt.getAddress();


                } catch (IOException e) {
                    System.out.println(e);
                    e.printStackTrace();
                }

                pkt = com_pkt.getData();
            }

            int cmd_id = get_int(pkt[1], pkt[2]);
            int client_current_seq = get_int(pkt[5], pkt[6]);
            int resp_id = get_int(pkt[3], pkt[4]);
            int the_nack = get_int(pkt[7], pkt[8]);

            //System.out.println("Command id = " + cmd_id);

            if( client_current_seq > client_seq_num + 1 ){

                byte[] tmp = {0,0};

                make_response(cmd_id, 0, 0, client_seq_num, tmp);

                send_response(0);

            }

            client_seq_num = client_current_seq;
            
            //IF CONNECT REQUEST RECEIVED
            if( cmd_id == 100){

                System.out.println("Connect Request received");

                byte[] temp = {0,0};

                make_response(cmd_id, 100, 1, 0, temp);

                //SEND CONNECCT SUCCESS
                send_response(1);

            }

            else if(cmd_id == 420){

                System.out.println("Command End Received!");
                System.exit(0);
            }

            //IF GET-IMG COMMAND RECEIVED
            else if( cmd_id == 201){


                if(resp_id == 0 & the_nack == 0){

                    System.out.println("Sending Image Secifications");

                    send_image(data_size, 2);
                }

                //IF CLIENT READY RECEIVED
                else if(resp_id == 211){

                    client_confirm_flag = true;

                    continue;
                }

                //IF NACK RECEIVED
                else if(the_nack != 0){

                    try {

                        System.out.println("NACK received = " + the_nack);

                        DatagramPacket dropped_pkt = packets.get(String.valueOf(the_nack));

                        my_skt.send(dropped_pkt);

                        //System.out.println("Dropped packet sent! seq = " + the_nack);

                    } catch (IOException e) {
                        System.out.println(e);
                        e.printStackTrace();
                    }

                }

            }
            
        }
    }




    public static void main(String[] args) {
        
        if(args.length > 0){

            my_port = Integer.parseInt(args[0]);

            data_size = Integer.parseInt(args[1]);

            image_path = args[2];

            try {

                my_skt = new DatagramSocket(my_port);

            } catch (SocketException e) {
                System.out.println(e);
                e.printStackTrace();
            }

            rover t1 = new rover();
            rover t2 = new rover();
            rover t3 = new rover();
            rover t4 = new rover();

            t1.start();
            t2.start();
            t3.start();
            t4.start();


        }
    }

}
