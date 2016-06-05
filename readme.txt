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
1. Open server.java file, hit run. Options will be listed in the console.
2. Open intermediate.java, hit run. Follow Console prompts to simulate errors.
3. Open Client.java, hit run.
4. Follow Prompts in console.
	i.	 If the server is on another computer enter "n" else enter "y"
			1. If no is selected enter the IP address of the server in the format X.X.X.X where x can be 1-3 digits long ex. 125.733.6.56
			2. If you do not know the IP address of the server go onto the server computer and Google "get my ip". It will be the first result
	i.   Choose (t)est to turn on test mode. 
	ii.  Choose (v)erbose mode to turn off verbose mode if you don't want detailed print statements about received and sent data packets.
	iii. Choose (r)ead or (w)rite.
5. Enter file name to start transfer.
 
File explanation:
- Server.java contains the server code.
- Client.java contains the client code.
- Intermediate.java contains intermediate host/error simulator code. Console will prompt you to simulate network errors (delay, duplicate, lose), packet errors (corrupted packets or invalid TID), or no errors.
- Message.java contains helper code for printing, formatting, and validating DatagramPackets.
- Stoppable.java contains code abstracted from the Client and Server classes.
- MalformedPacketException.java exists as a class to be thrown in the Message.validate function in order to avoid having to catch generic exceptions.
 
 
 
TEST INSTRUCTIONS
Errors simulated can be tested using the intermediate menu.
See above for instructions on how to test on multiple computers. (Client Menu)
I/O errors can be simulated using incorrect filenames, changing the permissions on a file, or having the project run on a disk that is partially/completely full.
The Client saves files in the topmost folder of the project.
The intermediate must be restarted for every error to be simulated. If not restarted it will only simulate one error.


The server will save the file with the filename provided to the client in the server folder (server/FILENAME).
Other pathways for both client and server will be handled as part of iteration 5.
To test corrupted error packets use the intermediates menu to choose corrupt/unknown source for an error packet then run with an I/O error.

New errors added to the intermediate in this iteration was the packet error described below:
1. corrupted error packet
	i. invalid filename- changes the filename of a RRQ or WRQ to "".

*packet errors directly deal with the packet itself whereas network errors deal with sending/not sending the packets and do not manipulate the data within the packets.

 
To test the program you can run to read or write with all different size of files:

1. test.txt is a file to test a large set of data (greater than 512 bytes and 255 blocks)
2. new.txt is a file to test a small set of data (less than 512 bytes)
3. exact.txt is a file with exactly 512 bytes
4. empty.txt is an empty file.
 
Once you run with a certain file you can find the copied file in server/FILENAME

NOTES:
All final diagrams are in the Iteration 5 Diagrams folder.

TEAM DECISIONS:
As a team we decided:
	1. To overwrite a file if it already exists for both the client and server. Therefore we do not send error code 6.
	2. It was useful to leave failed file transfers. We did this for testing purposes (to see in the file what happened ex. empty, half full etc.).
	3. To have the client and the intermediate run on the same machine whether the server is on the same machine or not.
	   We did this to be able to have multiple intermediate running (to choose different error situations) and to decrease
	   the amount of multi-threading.

RESPONSIBILITY BREAKDOWN

Liam - Updated the error simulator to simulate corrupted RRQ and WRQ with invalid filename, help with other code, testing, and updated the read me.
Pallavi - Updated UML diagrams, helped with the read me, drew the timing diagrams and helped with the code
Megan - edited the server and client to handle the simulated errors and I/O errors, added code to handle server on another computer, testing, and helped with the timing diagrams