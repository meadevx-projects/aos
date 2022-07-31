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
				//Note: Actual message is in byte format stored in buf
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
					pktObj = (Packet) AOSProject3.deserialize(byteBuffer);
					
					
						
					
					//Do message processing
					message_processing(pktObj);
					
					
										
					
				} catch (ClassNotFoundException e) {
					AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - CommServer Messsage Processing Catch Block3",AOSProject3.exception_dir+AOSProject3.exception_file);
					e.printStackTrace();
				}
				
				
				
			}
			
			
			

		}
		catch(IOException ex)
		{
			AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - CommServer Messsage Processing Catch Block4",AOSProject3.exception_dir+AOSProject3.exception_file);
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
		
		AOSProject3.write_output("Protocol used by Server:"+transport_protocol,true);
		
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
		
		
		int node_id = pktObj.sender_id;
		int file_num = pktObj.file_num;
		int request_num = pktObj.request_number;
		
		AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- file_num:"+file_num+"......................",true);
		
		Service srv = new Service();
		
		// Increment logical timestamp (max)
		AOSProject3.logical_timestamp = Math.max(pktObj.logical_timestamp, AOSProject3.logical_timestamp) + 1;
		
		// Increment vector timestamp (max)
		for (int i = 0; i < AOSProject3.no_of_nodes; i++) 
		{
			if(AOSProject3.local_node_id==i)
			{
				AOSProject3.vector_timestamp[AOSProject3.local_node_id] = Math.max(pktObj.vector_timestamp[AOSProject3.local_node_id], AOSProject3.vector_timestamp[AOSProject3.local_node_id]) + 1;
			}
			else
			{
				AOSProject3.vector_timestamp[i] = Math.max(pktObj.vector_timestamp[i], AOSProject3.vector_timestamp[i]);
			}				
		}
		
		AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- Received - "+pktObj.packet_type+"["+file_num+"]"+"("+pktObj.request_number+") : From "+pktObj.sender_id+" To "+pktObj.receiver_node_id+" | MyLogTS:"+AOSProject3.logical_timestamp+" PktLogTS:"+pktObj.logical_timestamp+" | MyVecTS:"+Arrays.toString(AOSProject3.vector_timestamp)+" PktVecTS:"+Arrays.toString(pktObj.vector_timestamp)+" ",true);
		
	
		//Lock Semaphore
		AOSProject3.semaphore_lock("server","Before message processing",file_num);
		
		
		//implement if read lock already given dont give again
		
		if(pktObj.packet_type.equals("MSG_REQUEST_READ_LOCK")==true)
		{
			AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- Received & Started Processing - "+pktObj.packet_type+"("+pktObj.request_number+") : From "+pktObj.sender_id+" To "+pktObj.receiver_node_id,true);
			
			if(AOSProject3.write_lock_flag[file_num] == false)
			{					
				//Grant Read Lock
				srv.grant_read_lock(node_id,file_num,request_num);								
			}				
		}
		else if(pktObj.packet_type.equals("MSG_GRANTED_READ_LOCK")==true)
		{
			AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- Received & Started Processing - "+pktObj.packet_type+"("+pktObj.request_number+") : From "+pktObj.sender_id+" To "+pktObj.receiver_node_id,true);
			
			//Check for extra locks and release if the msg comes after the quorum timer expires
			if(AOSProject3.quorum_timer_off_flag[file_num] == true)
			{			
				srv.send_read_deny_lock(node_id,file_num);
			}
			else
			{
				//Update readLockReceiveHashmap
				AOSProject3.readLockReceiveHashmapList.get(file_num).put(node_id, 1);		
				
				//Store Max version number from packet
				AOSProject3.versionNumberHashmapList.get(file_num).put(node_id, pktObj.version_number);
				
				//Compute max version number
				if(pktObj.version_number > AOSProject3.max_version_number[file_num])
				{
					AOSProject3.max_version_number[file_num] = pktObj.version_number;
					AOSProject3.my_n_value[file_num] = pktObj.ru_value;
				}
			}
			
		}
		else if(pktObj.packet_type.equals("MSG_DENY_READ_LOCK")==true)
		{
			AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- Received & Started Processing - "+pktObj.packet_type+"("+pktObj.request_number+") : From "+pktObj.sender_id+" To "+pktObj.receiver_node_id,true);
						
			//Reset readLockGrantHashmap
			AOSProject3.readLockGrantHashmapList.get(file_num).put(node_id,0);					

		}
		else if(pktObj.packet_type.equals("MSG_REQUEST_WRITE_LOCK")==true)
		{
			AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- Received & Started Processing - "+pktObj.packet_type+"("+pktObj.request_number+") : From "+pktObj.sender_id+" To "+pktObj.receiver_node_id,true);
			
			if(AOSProject3.write_lock_flag[file_num] == false && (srv.check_read_grant_hashmap_set(file_num)==false) )
			{
				
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- About to grant should be false (Server call) -- WriteLockFlag: "+AOSProject3.write_lock_flag[file_num],true);
				
				//Grant Write Lock			
				srv.grant_write_lock(node_id,file_num,request_num);							
			}
			

		}
		
		else if(pktObj.packet_type.equals("MSG_GRANTED_WRITE_LOCK")==true)
		{
			AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- Received & Started Processing - "+pktObj.packet_type+"("+pktObj.request_number+") : From "+pktObj.sender_id+" To "+pktObj.receiver_node_id,true);
			
			//Check for extra locks and release if the msg comes after the quorum timer expires
			if(AOSProject3.quorum_timer_off_flag[file_num] == true || (AOSProject3.my_request_number > request_num))
			{			
				srv.send_write_deny_lock(node_id,file_num);
			}			
			else
			{
				//Set writeLockReceiveHashmap
				AOSProject3.writeLockReceiveHashmapList.get(file_num).put(node_id, 1);		

				//Store Max version number from packet
				AOSProject3.versionNumberHashmapList.get(file_num).put(node_id, pktObj.version_number);
				
				//Compute max version number
				if(pktObj.version_number > AOSProject3.max_version_number[file_num])
				{
					AOSProject3.max_version_number[file_num] = pktObj.version_number;
					AOSProject3.my_n_value[file_num] = pktObj.ru_value;
				}
			}
		}
		else if(pktObj.packet_type.equals("MSG_DENY_WRITE_LOCK")==true)
		{
			AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- Received & Started Processing - "+pktObj.packet_type+"("+pktObj.request_number+") : From "+pktObj.sender_id+" To "+pktObj.receiver_node_id,true);
			
			//Reset write_lock_flag
			AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- C4 - WLF should be true, next operation false, rcv deny write lock, (Server call) -- WriteLockFlag: "+AOSProject3.write_lock_flag[file_num],true);
			AOSProject3.write_lock_flag[file_num]=false;
			AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- C4 - WLF should be false, rcv deny write lock, (Server call) -- WriteLockFlag: "+AOSProject3.write_lock_flag[file_num],true);
				
			//Reset writeLockGrantHashmap
			AOSProject3.writeLockGrantHashmapList.get(file_num).put(node_id,0);	

		}		
		else if(pktObj.packet_type.equals("MSG_RELEASE_READ_LOCK")==true)
		{
			AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- Received & Started Processing - "+pktObj.packet_type+"("+pktObj.request_number+") : From "+pktObj.sender_id+" To "+pktObj.receiver_node_id,true);
						
			//Reset readLockGrantHashmap
			AOSProject3.readLockGrantHashmapList.get(file_num).put(node_id,0);	
			
		}
		else if(pktObj.packet_type.equals("MSG_RELEASE_WRITE_LOCK")==true)
		{
			AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- Received & Started Processing - "+pktObj.packet_type+"("+pktObj.request_number+") : From "+pktObj.sender_id+" To "+pktObj.receiver_node_id,true);
			
			//Reset write_lock_flag
			AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- C5 - WLF should be true, next operation false, rcv release write lock, (Server call) -- WriteLockFlag: "+AOSProject3.write_lock_flag[file_num],true);
			AOSProject3.write_lock_flag[file_num]=false;
			AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- C5 - WLF should be false, rcv release write lock, (Server call) -- WriteLockFlag: "+AOSProject3.write_lock_flag[file_num],true);
		
			//Reset writeLockGrantHashmap
			AOSProject3.writeLockGrantHashmapList.get(file_num).put(node_id,0);	
		}	
		else if(pktObj.packet_type.equals("MSG_REQUEST_LATEST_FILE_READ")==true)
		{
			AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- Received & Started Processing - "+pktObj.packet_type+"("+pktObj.request_number+") : From "+pktObj.sender_id+" To "+pktObj.receiver_node_id,true);
			
			srv.send_read_response_latest_file(node_id,file_num);
			
		}
		else if(pktObj.packet_type.equals("MSG_REQUEST_LATEST_FILE_WRITE")==true)
		{	
			AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- Received & Started Processing - "+pktObj.packet_type+"("+pktObj.request_number+") : From "+pktObj.sender_id+" To "+pktObj.receiver_node_id,true);
			
			srv.send_write_response_latest_file(node_id,file_num);
					
		}
		else if(pktObj.packet_type.equals("MSG_RESPONSE_LATEST_FILE_READ")==true)
		{
			AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- Received & Started Processing - "+pktObj.packet_type+"("+pktObj.request_number+") : From "+pktObj.sender_id+" To "+pktObj.receiver_node_id,true);
			
			//Receive file and store it on disk
			String filename = AOSProject3.byteArrayToFile(pktObj.file_contents,pktObj.file_name,AOSProject3.filesystem_dir+"read_latest/");
			
			AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- File Received (MSG_RESPONSE_LATEST_FILE_READ): "+filename,true);
			
			//Set flag on receiving the file
			AOSProject3.wait_for_read_latest_file[file_num] = true;
			
			//Do not replace replica, save as temp file
			
		}
		else if(pktObj.packet_type.equals("MSG_RESPONSE_LATEST_FILE_WRITE")==true)
		{
			AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- Received & Started Processing - "+pktObj.packet_type+"("+pktObj.request_number+") : From "+pktObj.sender_id+" To "+pktObj.receiver_node_id,true);
			
			//Receive file and store it on disk
			String filename = AOSProject3.byteArrayToFile(pktObj.file_contents,pktObj.file_name,AOSProject3.filesystem_dir);
			
			AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- File Received (MSG_RESPONSE_LATEST_FILE_WRITE): "+filename,true);
			
			//Set flag on receiving the file
			AOSProject3.wait_for_write_latest_file[file_num] = true;
			
			//Replace replica			
			
		}
		else if(pktObj.packet_type.equals("MSG_SEND_UPDATED_FILE")==true)
		{
			AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- Received & Started Processing - "+pktObj.packet_type+"("+pktObj.request_number+") : From "+pktObj.sender_id+" To "+pktObj.receiver_node_id,true);
			
			//Receive file and store it on disk
			String filename = AOSProject3.byteArrayToFile(pktObj.file_contents,pktObj.file_name,AOSProject3.filesystem_dir);
			
			AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- File Received (MSG_SEND_UPDATED_FILE): "+filename,true);
			
			//Replace replica, Modify RU value and VN value
			
			//Update local variables
			AOSProject3.my_version_number[file_num] = pktObj.version_number;
			AOSProject3.my_ru_value[file_num] = pktObj.ru_value;
		}
		
		
		//Unlock Semaphore
		AOSProject3.semaphore_unlock("server","After message processing",file_num);	
		
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
					AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - CommServer Messsage Processing Catch Block1",AOSProject3.exception_dir+AOSProject3.exception_file);
					e.printStackTrace();
				}
	
			}

		}
		catch(IOException ex)
		{
			AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - CommServer Messsage Processing Catch Block2",AOSProject3.exception_dir+AOSProject3.exception_file);
			ex.printStackTrace();
		}
	}
	

}