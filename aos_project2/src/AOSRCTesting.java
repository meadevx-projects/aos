import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class AOSRCTesting {
	
	public static int no_of_nodes;
	public static int local_node_id;
	public static int no_of_cs_req;
	public static int mean_delay;
	public static int mean_duration;
	public static int logical_timestamp=0;
	public static int request_timestamp=0;
	public static int[] vector_timestamp;
	public static int[] request_vector_timestamp;
	public static boolean cs_violation_found=false;
		
	public static void main(String[] args)
	{
		local_node_id = 0;
		
		// Read Config File
	    System.out.println("\n--Reading Config file--");
		read_config();
		
		System.out.println("No. of Nodes:"+no_of_nodes);
		System.out.println("No. of CS requests per node : "+no_of_cs_req);
		System.out.println("Total CS Requests : "+no_of_nodes*no_of_cs_req);
		System.out.println("Total No. of lines in resource.txt (Expected): "+no_of_nodes*no_of_cs_req*2);
		
		System.out.println("\n--Reading Resource file--");
		System.out.println("Total No. of lines in resource.txt (Actual): "+read_file_no_of_lines("resource.txt"));
		
		System.out.println("\n--Reading CSViolation file--");
		System.out.println("Total No. of lines in csviolation.txt: "+read_file_no_of_lines("csviolation.txt"));
		
		//System.out.println("\n--Reading Resource file (Parse Vector Timestamp)--");
		//parse_resource_file("resource.txt");
		
		System.out.println("\n");
	}
	
	public static void parse_resource_file(String filename)
	{
		
		// Read Config File - Start
		//String configFile = "src/"+filename;
		String configFile = filename;
		BufferedReader br = null;
		String line = "";
		
		int[] prevLeftArray = new int[no_of_nodes];;
		
		int line_no=0;
		int prev_line_no=0;
	 
		try {
	 
				
			br = new BufferedReader(new FileReader(configFile));
			
			while ((line = br.readLine()) != null)
			{
						
				line_no++;
				
				if(line_no==1)
				{
					continue;
				}
				
				String[] tokens = line.split("#");
				
				//System.out.print("Line:"+line_no+" ");
				//System.out.print(tokens[0]);
				//System.out.println(tokens[2]);
				
				String arrayStr = tokens[2].substring(1, tokens[2].length() - 1);
				
				String[] vectorStrArray = arrayStr.split(",");
				int[] vectorIntArray = new int[vectorStrArray.length];
				for(int i = 0; i < vectorStrArray.length; i++) {
					vectorIntArray[i] = Integer.parseInt(vectorStrArray[i].trim());
				}
				
				
				
				if(line_no%2!=0)
				{
					
					//System.out.println(prev_line_no+":"+Arrays.toString(prevLeftArray));
					//System.out.println(line_no+":"+Arrays.toString(vectorIntArray));
					
					boolean nextEnteredGreater = false;
					
					for(int i = 0; i < no_of_nodes; i++)
					{
						if((prevLeftArray[i] < vectorIntArray[i]) || (prevLeftArray[i] == vectorIntArray[i]))
						{
							if((prevLeftArray[i] < vectorIntArray[i]))
							{
								nextEnteredGreater = true;
								//System.out.println("flag set");
							}
						}
						else
						{
							//System.out.println("Line:"+line_no);
						}
					}
					
					//System.out.println("flag:"+nextEnteredGreater);
					
					if(nextEnteredGreater==false)
					{
						cs_violation_found = true;
						System.out.println("CS Violation Found - Line:"+line_no);
					}
				}
				
		
				prevLeftArray = vectorIntArray.clone();
				prev_line_no = line_no;
				
				
				
				
				
			}
			
			
			if(cs_violation_found==true)
			{
				System.out.println("CS Violation(s) Found"); 
			}
			else
			{
				System.out.println("No CS Violation(s) Found"); 
			}
			
 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		// Read Config File - End
		
		
		
		
	}
	
	public static int read_file_no_of_lines(String filename)
	{
		
		// Read Config File - Start
		//String configFile = "src/"+filename;
		String configFile = filename;
		BufferedReader br = null;
		String line = "";

		int line_no=0;
	 
		try {
	 
				
			br = new BufferedReader(new FileReader(configFile));
			
			while ((line = br.readLine()) != null) {
						
				line_no++;
			}
			
 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		// Read Config File - End
		
		
		return line_no;
		
	}
	
	public static void read_config()
	{
		
		// Read Config File - Start
		//String configFile = "src/config.txt";
		String configFile = "config.txt";
		BufferedReader br = null;
		String line = "";

	 
		try {
	 
			int line_no=1;
				
			boolean line_no_of_nodes=false;
			boolean line_no_of_cs_req=false;
			boolean line_mean_delay=false;
			boolean line_mean_duration=false;
			
			br = new BufferedReader(new FileReader(configFile));
			
			while ((line = br.readLine()) != null) {
				
				if(line.contains("Number of nodes")==true)
				{
					line_no_of_nodes=true;
					continue;					
				}
				else if(line.contains("Number of critical section requests per node")==true)
				{							
					line_no_of_cs_req=true;
					continue;
				}
				else if(line.contains("Mean delay between two consecutive critical section requests")==true)
				{							
					line_mean_delay=true;
					continue;
				}
				else if(line.contains("Mean duration of critical section")==true)
				{							
					line_mean_duration=true;
					continue;
				}
	 
				if(line.startsWith("#")==false || line.startsWith(" ")==false)
				{				
					if(line_no_of_nodes==true)
					{
						no_of_nodes = Integer.parseInt(line.trim());
						//System.out.println("No. of Nodes:"+no_of_nodes);
						line_no_of_nodes=false;
						vector_timestamp = new int[no_of_nodes];
						request_vector_timestamp = new int[no_of_nodes];
					}
					else if(line_no_of_cs_req==true)
					{
						//System.out.println("Read - Number of critical section requests per node : "+line.trim());
						no_of_cs_req = Integer.parseInt(line.trim());
						line_no_of_cs_req=false;
					}
					else if(line_mean_delay==true)
					{
						//System.out.println("Read - Mean delay between two consecutive critical section requests : "+line.trim());
						mean_delay = Integer.parseInt(line.trim());
						line_mean_delay=false;
					}
					else if(line_mean_duration==true)
					{
						//System.out.println("Read - Mean duration of critical section : "+line.trim());
						mean_duration = Integer.parseInt(line.trim());
						line_mean_duration=false;
					}
						
				}
						
		
				line_no++;
			}
			
			
			
			
	 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		// Read Config File - End
		
		
	}
	

}
