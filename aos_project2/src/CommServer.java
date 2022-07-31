import java.io.*;
import java.net.*;

import com.sun.nio.sctp.*;

import java.nio.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

public class CommServer extends Thread
{
	public static final int MESSAGE_SIZE = 1000;
	
	private int server_port;
	private String transport_protocol;
	
	boolean flag_terminate = false;
	
	public CommServer(int node_portx, String protocolx)
	{
		this.server_port = node_portx;
		this.transport_protocol = protocolx;
	}
	
	//--SCTP Server--
	public void go_sctp()
	{
		byte[] bytes = new byte[MESSAGE_SIZE];
		
		//Buffer to hold messages in byte format
		ByteBuffer byteBuffer = ByteBuffer.allocate(MESSAGE_SIZE);
		
		//String message;
		
		try
		{
			//Open a server channel
			SctpServerChannel sctpServerChannel = SctpServerChannel.open();
			
			//Create a socket addess in the current machine at port XXXX
			InetSocketAddress serverAddr = new InetSocketAddress(server_port);
			
			//Bind the channel's socket to the server in the current machine at port XXXX
			sctpServerChannel.bind(serverAddr);
			
			//Server goes into a permanent loop accepting connections from clients			
			while(true)
			{

												
				//Listen for a connection to be made to this socket and accept it
				//The method blocks until a connection is made
				//Returns a new SCTPChannel between the server and client
				SctpChannel sctpChannel = sctpServerChannel.accept();
				
				//Receive message in the channel (byte format) and store it in buf
				//Note: Actual message is in byre format stored in buf
				//MessageInfo has additional details of the message
				MessageInfo messageInfo = sctpChannel.receive(byteBuffer,null,null);
				
				//Just seeing what gets stored in messageInfo
				//System.out.println(messageInfo);
				//Converting bytes to string. This looks nastier than in TCP
				//So better use a function call to write once and forget it :)
				
				
				//message = byteToString(byteBuffer);
				/*
				//Finally the actual message
				System.out.println(message);
				*/
			
				// Retrieve all bytes in the buffer
				byteBuffer.clear();

				
				try {
					
					Packet pktObj = new Packet();
					pktObj = (Packet) AOSProject2.deserialize(byteBuffer);
					
					
					//Do message processing
					message_processing(pktObj);
									
					
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
				
				
				
			}
			
			
			

		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
	}

	public String byteToString(ByteBuffer byteBuffer)
	{
		byteBuffer.position(0);
		byteBuffer.limit(MESSAGE_SIZE);
		byte[] bufArr = new byte[byteBuffer.remaining()];
		byteBuffer.get(bufArr);
		return new String(bufArr);
	}

	public void run(){
		
		AOSProject2.write_output("Protocol used by Server:"+transport_protocol,true);
		
		if(transport_protocol=="SCTP")
		{
			go_sctp();
		}
		else
		{
			go_tcp();
		}
		
	}
	
	/*
	public static void main(String args[])
	{		
		CommServer CommServerObj = new CommServer(5000);
		CommServerObj.go();
	}
	*/
	
	
	
	//--Message Processing--
	public void message_processing(Packet pktObj)
	{
					
		// Increment logical timestamp (max)
		AOSProject2.logical_timestamp = Math.max(pktObj.logical_timestamp, AOSProject2.logical_timestamp) + 1;
		
		// Increment vector timestamp (max)
		for (int i = 0; i < AOSProject2.no_of_nodes; i++) 
		{
			if(i==AOSProject2.local_node_id)
			{
				AOSProject2.vector_timestamp[AOSProject2.local_node_id] = Math.max(pktObj.vector_timestamp[AOSProject2.local_node_id], AOSProject2.vector_timestamp[AOSProject2.local_node_id]) + 1;
			}
			else
			{
				AOSProject2.vector_timestamp[i] = Math.max(pktObj.vector_timestamp[i], AOSProject2.vector_timestamp[i]);
			}				
		}
		
		AOSProject2.write_output("Node:"+AOSProject2.local_node_id+" Received -- "+pktObj.packet_type+" : From "+pktObj.sender_id+" To "+pktObj.receiver_node_id+" MyReqTS:"+AOSProject2.request_timestamp+" PktReqTS:"+pktObj.request_timestamp+" | MyLogTS:"+AOSProject2.logical_timestamp+" PktLogTS:"+pktObj.logical_timestamp+"  ",true);
		
		AOSProject2.write_output("Node:"+AOSProject2.local_node_id+" Received -- "+pktObj.packet_type+" : From "+pktObj.sender_id+" To "+pktObj.receiver_node_id+" MyReqVecTS:"+Arrays.toString(AOSProject2.request_vector_timestamp)+" PktReqVecTS:"+Arrays.toString(pktObj.request_vector_timestamp)+" | MyVecTS:"+Arrays.toString(AOSProject2.vector_timestamp)+" PktVecTS:"+Arrays.toString(pktObj.vector_timestamp)+" ",true);
		
		//Acquire semaphore lock
		try {
			AOSProject2.semaphore.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if(pktObj.packet_type.equals("MSG_CS_REQUEST")==true || pktObj.packet_type.equals("MSG_REQUEST_KEY")==true)
		{
			
			if(AOSProject2.cs_request_flag==true)
			{
				// Packet request_timestamp < AOSProject2.request_timestamp
				// Tie break if Packet request_timestamp = AOSProject2.request_timestamp by giving preference to lower Node ID
				if( ( (pktObj.request_timestamp < AOSProject2.request_timestamp) || ( (pktObj.request_timestamp == AOSProject2.request_timestamp) && (pktObj.sender_id < AOSProject2.local_node_id) ) ) && (AOSProject2.cs_section_flag==false) )
				{
					AOSProject2.write_output("Node:"+AOSProject2.local_node_id+" (send_key) called from cs_request_flag=true",true);
					
					int client_node_id = pktObj.sender_id;
					
					//Check if this node has the key
					if(AOSProject2.NodeKeysInfo.get(client_node_id)==1)
					{	
						Service srv = new Service();
						srv.send_key(client_node_id);
						
						// Request again to get the key back					
						srv.request_key(client_node_id);					
					}
				}
				else
				{
					//queue			
					AOSProject2.queue.add(pktObj);
					
					int node_id = pktObj.sender_id;
					
					AOSProject2.write_output("Node:"+AOSProject2.local_node_id+" - Node:"+node_id+" added to queue",true);
					
				}
			}
			else
			{
				AOSProject2.write_output("Node:"+AOSProject2.local_node_id+" cs_request_flag value:"+AOSProject2.cs_request_flag,true);
				
				// Give up the key if this node has not made CS Request
				
				AOSProject2.write_output("Node:"+AOSProject2.local_node_id+" (send_key) called from give up key",true);
				
				int client_node_id = pktObj.sender_id;
				
				//Check if this node has the key
				if(AOSProject2.NodeKeysInfo.get(client_node_id)==1)
				{	
					Service srv = new Service();
					srv.send_key(client_node_id);
				}
			}
						
		}	
		else if(pktObj.packet_type.equals("MSG_SEND_KEY")==true)
		{
			int client_node_id = pktObj.sender_id;
			
			// Set Key
			AOSProject2.NodeKeysInfo.put(client_node_id, 1);
					
		}
		
		//Release semaphore lock
		AOSProject2.semaphore.release();
	}
	
	
	
	
	
	//--TCP Server--
	public void go_tcp()
	{
		
		try
		{
			//Create a server socket at port 5000
			ServerSocket serverSock = new ServerSocket(server_port);
			
			//Server goes into a permanent loop accepting connections from clients			
			while(true)
			{
				try {
				//Listens for a connection to be made to this socket and accepts it
				//The method blocks until a connection is made
					Socket sock = serverSock.accept();
					
					ObjectInputStream input = new ObjectInputStream(sock.getInputStream());
	
					Packet pktObj = new Packet();
					
					pktObj = (Packet) input.readObject();
					
					//Do message processing
					message_processing(pktObj);
					
					
				
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	
			}

		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
	}
	

}