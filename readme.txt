README

Team Members:
Liam Close - 100939063
Megan Ardron - 100957245
Pallavi Singh - 100954022

Steps to set up the project:
	1. Download the files and save them in the workspace
	2. Open Eclipse.
	3. File -> Import -> General -> Existing Projects into Workspace -> Next -> Select root directory where files were saved in step 1 -> Finish.

Steps to run the project:
	1. Open server.java file, hit run.
	2. Open intermediate.java, hit run.
	3. Open Client.java, hit run.
    4. Follow Prompts in console
		i.   Choose (o)ptions to change to turn Verbose mode off and to choose whether to put in test mode or not. 
		ii.  Choose Verbose mode if you want detailed prints statments about received and sent data packets.
		iii. Choose test mode if you want it to go directly from Client to Server rather than through the error simulator (Intermediate).
	5. Enter file name	 
		
File explanation:
	- Server.java contains the server code.
	- Client.java contains the client code.
	- Intermediate.java contains intermediate host/error simulator code.
	- Message.java contains helper code for printing and formatting and validating DatagramPackets.
	- Stoppable.java contains code abstracted from the Client and Server classes
	
	
	
	
TEST INSTRUCTIONS
	
To test the program you can run to read or write with all defferent size of files:
	1. test.txt is a file to test a large set of data (greater than 512 bytes and 255 blocks)
	2. new.txt is a file to test a small set of data (less than 512 bytes)
	3. exact.txt is a file with exactly 512 bytes
	4. empty.txt is an empty file.
	
Once you run with a certain file you can find the copied file in server/FILENAME


RESPONSIBILITY BREAKDOWN

Liam - Created the UCM diagrams, this text file and helped write code
Pallavi - Created UML diagrams, helped with this text file and helped write code
Megan - Wrote the code 