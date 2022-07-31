import java.io.*;
import java.net.*;

import com.sun.nio.sctp.*;

import java.nio.*;

public class CommClient
{
	public static final int MESSAGE_SIZE = 1000;
	
	private String transport_protocol;
	
	public CommClient(String protocolx)
	{
		this.transport_protocol = protocolx;
	}
	
	
	@SuppressWarnings("unused")
	private boolean isLocalPortInUse(int port) {
	    try {
	        // ServerSocket try to open a LOCAL port
	        new ServerSocket(port).close();
	        // local port can be opened, it's available
	        return false;
	    } catch(IOException e) {
	        // local port cannot be opened, it's in use
	    	AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - Port in use",AOSProject3.exception_dir+AOSProject3.exception_file);
	        return true;
	    }
	}
	
	public void go_sctp(int node_id, String node_name, int node_port, Packet pktObj)
	{
		//Buffer to hold messages in byte format
		ByteBuffer byteBuffer = ByteBuffer.allocate(MESSAGE_SIZE);
		
		//String message = "Hello from clientM from "+sender_node_name+" to "+node_name;
		
		try
		{
			//Create a socket address for  server at net01 at port 5000
			SocketAddress socketAddress = new InetSocketAddress(node_name,node_port);
			
			//Open a channel. NOT SERVER CHANNEL
			SctpChannel sctpChannel = SctpChannel.open();
			
			//Bind the channel's socket to a local port. Again this is not a server bind
			/*int local_port = 8734;
			while(isLocalPortInUse(local_port)==true)
			{
				System.out.println("Local Port In Use:"+local_port);
				local_port++;
			}
			sctpChannel.bind(new InetSocketAddress(local_port));
			*/
			
			//Connect the channel's socket to  the remote server
			sctpChannel.connect(socketAddress);
			
			//Before sending messages add additional information about the message
			MessageInfo messageInfo = MessageInfo.createOutgoing(null,0);
			
			//convert the string message into bytes and put it in the byte buffer
			byteBuffer.put(AOSProject3.serialize(pktObj));
			
			//Reset a pointer to point to the start of buffer 
			byteBuffer.flip();
			
			//Send a message in the channel (byte format)
			sctpChannel.send(byteBuffer,messageInfo);
			
			//Close Channel - added
			sctpChannel.close();
		}
		catch(IOException ex)
		{
			AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - CommClient - GoSCTP",AOSProject3.exception_dir+AOSProject3.exception_file);
			AOSProject3.write_output("111 - Node:"+AOSProject3.local_node_id+" -- "+pktObj.packet_type+"["+pktObj.file_num+"] : From "+pktObj.sender_id+" To "+pktObj.receiver_node_id,true);
			ex.printStackTrace();
		}
	}
	
	public void go(int node_id, String node_name, int node_port, Packet pktObj)
	{
		AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" Sent -- "+pktObj.packet_type+"["+pktObj.file_num+"]("+pktObj.request_number+") : From "+pktObj.sender_id+" To "+pktObj.receiver_node_id,true);
		
		if(transport_protocol=="SCTP")
		{
			go_sctp(node_id, node_name, node_port, pktObj);
		}
		else
		{
			go_tcp(node_id, node_name, node_port, pktObj);
		}
		
	}
	
	public void run(){
	     //  go_sctp();
	}
	
	/*
	public static void main(String args[])
	{
		CommClient CommClientObj = new CommClient(0,localhost,5000);
		CommClientObj.go();
	}
	*/
	
	public void go_tcp(int node_id, String node_name, int node_port, Packet pktObj)
	{
		//String message;
		try
		{
			//Create a client socket and connect to server at 127.0.0.1 port 5000
			Socket clientSocket = new Socket();
			
			clientSocket.connect(new InetSocketAddress(node_name,node_port), 0);
			
			ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
			
			output.writeObject(pktObj);
			
			output.flush();
			output.close();	
			
			//Close Channel - added
			clientSocket.close();
		}
		catch(IOException ex)
		{
			AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - CommClient - GoTCP",AOSProject3.exception_dir+AOSProject3.exception_file);
			ex.printStackTrace();
		}
	}
}
