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
	i.   Choose (t)est to turn on test mode. 
	ii.  Choose (v)erbose mode to turn off verbose mode if you don't want detailed print statments about received and sent data packets.
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
I/O errors can be simulated using incorrect filenames, changing the permissions on a file, or having the project run on a disk that is partially/completely full.
The Client needs to be restarted for every new file being transferred. It saves files in the topmost folder of the project.
The intermediate must be restarted for every error to be simulated. If not restarted it will only simulate one error.

The server will save the file with the filename provided to the client in the server folder (server/FILENAME).
Other pathways for both client and server will be handled as part of iteration 5.
To test corupted error packets use the intermediates menu to choose corrupt/unknown source for an error packet then run with an I/O error.

New errors added to the intermediate in this iteration were the packet errors described below:
1. corrupted error packet
	i. invalid opcode- changes the opcode to an opcode that is not 1,2,3,4,5
	ii. invalid error code- changes the error code to an error code that is not 1,2,3,4,5,6
	iii. no null terminator- removes the null terminator from a Error Message

2.unknown source packet- sends a Error packet from an unknown source (port)


*packet errors directly deal with the packet itself whereas network errors deal with sending/not sending the packets and do not manipulate the data within the packets.

 
To test the program you can run to read or write with all different size of files:

1. test.txt is a file to test a large set of data (greater than 512 bytes and 255 blocks)
2. new.txt is a file to test a small set of data (less than 512 bytes)
3. exact.txt is a file with exactly 512 bytes
4. empty.txt is an empty file.
 


Once you run with a certain file you can find the copied file in server/FILENAME

NOTES:
The timing diagrams for Iteration 3 are unchanged, we didn't have time to edit them for this iteration. They will be updated for iteration 5.
The new diagrams are in the Iteration 4 Diagrams folder.

As a team we decided to overwrite a file if it already exists for both the client and server. So we do not send error code 6.
We also decided that it was useful to leave failed file transfers for testing purposes.

RESPONSIBILITY BREAKDOWN


Liam - Updated the error simulator to simulate both corupted error packets and error packets sent from an unknown source, testing, and updated the read me.
Pallavi - Updated UML diagrams, helped with the read me, drew the timing diagrams and helped with the code
Megan - edited the server and client to handle the simulated errors and I/O errors, testing, and helped with the timing diagrams