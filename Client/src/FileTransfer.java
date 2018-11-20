import java.io.*;
import java.util.Arrays;

public class FileTransfer extends ClientSocketEx{
private String			_savedir = "";
private int 			_lastpercentage_len = 0;

// return value: 0 - successful; 1 - transfer ok, but file IO error; 2 - transfer or connection failed;
//				3 - file does not exist in the server
public int receive_data(String filename) {
	if (_socket == null) {
		System.out.println("no socket");
		return 2;
	}
	
	DataInputStream inputStream = null;
	
	try {
		// retreve the stream
		inputStream = _socket.getInputStream();
		
	} catch (Exception e) {
		System.out.println("getInputStream() failed");
		return 2;
	}
	
	try {
		String msg_received;
		// send verification information
		_socket.send_msg("i love ex machina");
		
		// get hands shaked
		msg_received = _socket.receive_msg();
		if (msg_received.equals("machina")) {
			System.out.println("get machina. cool!");
		} else {
			System.out.println("hand shake failed.");
			_socket.disconnect();
			return 2;
		}
		
		// send ur real need
		_socket.send_msg("need a file");
//		_socket.send_msg("list all files");
		msg_received = _socket.receive_msg();
		
		// send required filename
		_socket.send_msg(filename);
		
	     // 接收part1, which is filename
	     String fullname = _socket.receive_msg();
         _socket.send_msg("ok");
	     if (!fullname.equals(filename))
	     {
		     System.out.println(fullname);
		     System.out.println("file does not exist in the server.\n");
			 _socket.disconnect();
			 return 3;
	     }
	     
	     System.out.println(fullname + " is good.");
	     fullname = _savedir + fullname; 

	     // receive part2, which is file size. 
	     long filesize = Integer.parseInt(_socket.receive_msg());
	     _socket.send_msg("ok");
	     System.out.println("file length: " + filesize + "\n");

	     // 用于接收二进制文件流的stream, get ready for receiving the third part, which is the binary content
	     DataOutputStream fileStream = null;
	     try {
		     FileOutputStream fileoutputStream = new FileOutputStream(fullname);
		     BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileoutputStream);
		     fileStream = new DataOutputStream(bufferedOutputStream);
    	 } catch (Exception e1) {
			try {
				inputStream.close();
			} catch (IOException e2) {
				e1.printStackTrace();
			}
			System.out.println("file IO errer: " + fullname);
			return 1;
    	 }
	     
	     System.out.println("receiving file...");
	     
	     // make some room for showing the progress percentage
	     System.out.print("transfering:     ");
	     _lastpercentage_len = 0;
	     String percent_show = "what?";
	     
	     int receive_total = 0;
	     int bufferSize = 16384;
	     byte[] receive_buff = new byte[bufferSize];
	     // start to receive
	     while (true) {
	    	 int receive_current = 0;
	    	 if (inputStream != null) {
	    		 // beware that buffsize. only receive that amount of data each time
	    		 receive_current = inputStream.read(receive_buff);
	    	 }
	    	 
	    	 if (receive_current != -1){
	    		 receive_total += receive_current;
	    	 } else {
	    		 break;
	    	 }

	    	 // just save those binary to file
	    	 try {
	    		 fileStream.write(receive_buff, 0, receive_current);
	    	 } catch (Exception e1) {
    			try {
    				inputStream.close();
    			} catch (IOException e2) {
    				e1.printStackTrace();
    			}
    			System.out.println("file IO errer: " + fullname);
    			return 1;
	    	 }
	    	 
	    	 percent_show = 100.0 * receive_total / filesize + "%";
	    	 for (int i=0; i<_lastpercentage_len; i++) {
	    		 System.out.print('\b');
	    	 }
	    	 System.out.print(percent_show);
	    	 for (int i=percent_show.length(); i<_lastpercentage_len; i++) {
	    		 System.out.print(' ');
	    	 }
	    	 for (int i=percent_show.length(); i<_lastpercentage_len; i++) {
	    		 System.out.print('\b');
	    	 }
	    	 _lastpercentage_len = percent_show.length();
	     }
	     
	     fileStream.close();
    	 for (int i=0; i<_lastpercentage_len; i++) {
    		 System.out.print('\b');
    	 }
	     
	     if (receive_total == filesize) {
	    	 System.out.print("100%\n");
	    	 System.out.println("\nfile received: " + fullname + "\n");
	     } else {
	    	 System.out.println("\nonly partial file was received!!! " + "file saved to " + fullname + "\n");
	     }
	} catch (Exception e) {
		try {
			inputStream.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		System.out.println("oh boy, don't know where to start it." + "\n");
		return 2;
	}

	try {
		inputStream.close();
	} catch (IOException e1) {
		e1.printStackTrace();
	}

	return 0;
}

public FileTransfer(String savedir) {
	_savedir = savedir;
	return;
}

/*
public static void main(String arg[]) {
	FileTransfer fileTransfer = new FileTransfer("c:\\Users\\ziken\\Desktop\\");
//	FileTransfer fileTransfer = new FileTransfer("//Users//ziken//Desktop//");
	
	try {
		if (fileTransfer.connect("localhost", 8821)) {
			fileTransfer.receive_data("_DSC7688.jpg");
//			fileTransfer.receive_data("Homework 1.docx");			
			fileTransfer.disconnect();
		}
	} catch (Exception ex) {
		ex.printStackTrace();
	}
}
*/

}
