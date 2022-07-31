import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;


public class AOSDVTesting {
	
	public static int local_node_id;
	
	public static int no_of_nodes;
	public static int no_of_files;
	public static int no_of_op;
	public static int mean_delay;
	public static int fraction_read;
	public static int exp_backoff_min;
	public static int exp_backoff_max;
	public static int exp_last_max;
	
	public static String filename;
	
	public static int logfile_total_no_of_lines;
	public static int actual_op_read;
	public static int actual_op_write;
	
	public static String currentWorkingDir;
	
	public static int logical_timestamp=0;
	public static int[] vector_timestamp;

	public static boolean cs_violation_found=false;
	public static boolean exception_found=false;
		
	public static void main(String[] args)
	{
		local_node_id = 0;
		
		// Read Config File
	    System.out.println("\n--Reading Config file--");
		read_config();
		
		//No. of Read Operations
		int op_read = (no_of_op * fraction_read)/100;
		
		//No. of Write Operations
		int op_write = no_of_op - op_read;
		
		System.out.println("\n--Configuration--");
		System.out.println("No. of Nodes: "+no_of_nodes);
		System.out.println("No. of Files: "+no_of_files);
		System.out.println("No. of CS requests per node: "+no_of_op);
		System.out.println("Fraction of Read Operations: "+fraction_read+"%");
		System.out.println("Fraction of Write Operations: "+(100-fraction_read)+"%");
		
		System.out.println("\n--Expected Values--");
		System.out.println("Total No. of lines (Expected): "+no_of_nodes*no_of_op*2);
		System.out.println("Total CS Requests (Expected): "+no_of_nodes*no_of_op);	
		System.out.println("Total No. of Read Operations (Expected): "+op_read*no_of_nodes);
		System.out.println("Total No. of Write Operations (Expected): "+op_write*no_of_nodes);
			
		//Process Log files
		process_log_files();
		System.out.println("\n--Reading Log files--");
		System.out.println("Total No. of lines (Actual): "+logfile_total_no_of_lines);
		System.out.println("Total CS Requests (Actual): "+(logfile_total_no_of_lines/2));
		System.out.println("Total No. of Read Operations (Actual): "+actual_op_read);
		System.out.println("Total No. of Write Operations (Actual): "+actual_op_write);
		
		//Process csviolation files
		System.out.println("\n--Reading CSViolation files--");
		process_csviolation_files();
		
		//Process exception files
		System.out.println("\n--Reading Exception files--");
		process_exception_files();
			
		System.out.println("\n");
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
	
	public static void process_exception_files()
	{
		int total_lines=0;
		int total_lines_read=0;
		int total_lines_write=0;
		
		for(int i=0;i<no_of_nodes;i++)
		{
		
			filename = "exception"+i+".txt";
			
			//String filepath = "src/exception/"+filename;
			String filepath = "exception/"+filename;
			
			File exceptionfile = new File(filepath);
			
			if(exceptionfile.exists()==true)
			{
				System.out.println("Exception found for Node-"+i);
				
				exception_found = true;
			}
		}
		
		if(exception_found == false)
		{
			System.out.println("No Exceptions found");
		}
		
	}
	
	public static void process_csviolation_files()
	{
		int total_lines=0;
		int total_lines_read=0;
		int total_lines_write=0;
		
		for(int i=0;i<no_of_files;i++)
		{
		
			filename = "csviolation"+i+".txt";
			
			//String filepath = "src/csviolation/"+filename;
			String filepath = "csviolation/"+filename;
			
			File csviolationfile = new File(filepath);
			
			if(csviolationfile.exists()==true)
			{
				System.out.println("CS Violation found for File"+i);
				
				cs_violation_found = true;
			}
		}
		
		if(cs_violation_found == false)
		{
			System.out.println("No CS Violations found");
		}
		
	}
	
	
	public static void process_log_files()
	{
		int total_lines=0;
		int total_lines_read=0;
		int total_lines_write=0;
		
		for(int i=0;i<no_of_files;i++)
		{
		
			filename = "log"+i+".txt";
			
			//String filepath = "src/log/"+filename;
			String filepath = "log/"+filename;
			
			File logfile = new File(filepath);
			
			if(logfile.exists()==true)
			{
				// Read Config File - Start
				//String configFile = "src/"+filename;
				String configFile = filepath;
				BufferedReader br = null;
				String line = "";
		
				int line_no=0;
				int line_no_read=0;
				int line_no_write=0;
			 
				try {
			 
						
					br = new BufferedReader(new FileReader(configFile));
					
					while ((line = br.readLine()) != null) {
								
						line_no++;
						
						if(line.contains("reading")==true)
						{
							line_no_read++;
						}
						
						if(line.contains("writing")==true)
						{
							line_no_write++;
						}
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
				
				total_lines = total_lines + line_no;
				total_lines_read = total_lines_read + line_no_read;
				total_lines_write = total_lines_write + line_no_write;
			}
		}
		
		logfile_total_no_of_lines = total_lines;
		actual_op_read = total_lines_read/2;
		actual_op_write = total_lines_write/2;
		
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
			boolean line_location=false;
			boolean line_no_of_files=false;			
			boolean line_no_of_op=false;
			boolean line_mean_delay=false;
			boolean line_fraction_read=false;
			boolean line_exp_backoff=false;
			
			br = new BufferedReader(new FileReader(configFile));
			
			while ((line = br.readLine()) != null) {
				
				//System.out.println(line);
				
				if(line.contains("Number of nodes")==true)
				{
					line_no_of_nodes=true;
					continue;					
				}
				else if(line.contains("Identifier	Hostname	Port")==true)
				{
					line_location=true;
					continue;
				}
				else if(line.contains("Numbers of files in the file system")==true)
				{
					line_location=false;
					
					line_no_of_files=true;
					continue;
				}
				else if(line.contains("Number of operations executed per node")==true)
				{							
					line_no_of_op=true;
					continue;
				}
				else if(line.contains("Mean delay between two consecutive operations (assume exponentially distributed)")==true)
				{							
					line_mean_delay=true;
					continue;
				}
				else if(line.contains("Fraction of read operations")==true)
				{							
					line_fraction_read=true;
					continue;
				}
				else if(line.contains("Parameters of exponential backoff (minimum and maximum waiting times)")==true)
				{							
					line_exp_backoff=true;
					continue;
				}
	 
				if(line.startsWith("#")==false || line.startsWith(" ")==false)
				{				
					if(line_no_of_nodes==true)
					{
						no_of_nodes = Integer.parseInt(line.trim());
						System.out.println("Read - No. of Nodes:"+no_of_nodes);
						line_no_of_nodes=false;
						vector_timestamp = new int[no_of_nodes];
					}
					else if(line_no_of_files==true)
					{						
						no_of_files = Integer.parseInt(line.trim());
						System.out.println("Read - Numbers of files in the file system : "+no_of_files);
						line_no_of_files=false;
					}
					else if(line_no_of_op==true)
					{						
						no_of_op = Integer.parseInt(line.trim());
						System.out.println("Read - Number of operations executed per node : "+no_of_op);
						line_no_of_op=false;
					}
					else if(line_mean_delay==true)
					{						
						mean_delay = Integer.parseInt(line.trim());
						System.out.println("Read - Mean delay between two consecutive operations (assume exponentially distributed): "+mean_delay);
						line_mean_delay=false;
					}
					else if(line_fraction_read==true)
					{					
						fraction_read = Integer.parseInt(line.trim());
						System.out.println("Read - Fraction of read operations: "+fraction_read);
						line_fraction_read=false;
					}
					else if(line_exp_backoff==true)
					{					
						exp_backoff_min = Integer.parseInt(line.split("\t")[0].trim());
						exp_backoff_max = Integer.parseInt(line.split("\t")[1].trim());
						exp_last_max = exp_backoff_min;
						System.out.println("Read - Exponential Backoff Min: "+exp_backoff_min);
						System.out.println("Read - Exponential Backoff Max: "+exp_backoff_max);
						line_exp_backoff=false;
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
