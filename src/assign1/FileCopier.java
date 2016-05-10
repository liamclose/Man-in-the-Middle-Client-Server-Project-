package assign1;

import java.io.*;

/**
 * FileCopier.java - Demonstrates how to use Java's byte stream I/O
 * classes to copy a file. Copies the contents of in.dat to out.dat.
 *
 * @version 1.1 February 26, 2002
 */
public class FileCopier
{
    public static void main(String[] args)
    throws FileNotFoundException, IOException
    {
        /*
         * A FileInputStream object is created to read the file
         * as a byte stream. A BufferedInputStream object is wrapped
         * around the FileInputStream, which may increase the
         * efficiency of reading from the stream.
         */
        BufferedInputStream in = 
            new BufferedInputStream(new FileInputStream("C:/Users/Megan/workspace/test.txt"));
    	//File in = new File("text.txt");
        /*
         * A FileOutputStream object is created to write the file
         * as a byte stream. A BufferedOutputStream object is wrapped
         * around the FileOutputStream, which may increase the
         * efficiency of writing to the stream.
         */
        BufferedOutputStream out =
            new BufferedOutputStream(new FileOutputStream("out.txt"));

        byte[] data = new byte[512];
        int n;
        
        /* Read the file in 512 byte chunks. */
        while ((n = in.read(data)) != -1) {
            /* 
             * We just read "n" bytes into array data. 
             * Now write them to the output file. 
             */
            out.write(data, 0, n);
        }
        in.close();
        out.close();
    }
}
