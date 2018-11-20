package com.example.ziken.dirmonitor;

import android.content.Context;

import com.example.ziken.dirmonitor.ClientSocketEx;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;


import java.io.IOException;
import java.util.Arrays;

public class FileTransfer extends ClientSocketEx {
private String			_savedir = "";
private int _filesize = -1;
private int 			_lastpercentage_len = 0;
private Context _ctx = null;

public FileTransfer(String savedir, Context ctx) {
    _savedir = savedir;
    _ctx = ctx;
    return;
}

public String get_filelist() {
    if (_socket == null) {
        System.out.println("no socket");
        return "no socket";
    }

    String msg_received = "";
    try {
        // send verification information
        _socket.send_msg("i love ex machina");

        // get hands shaked
        msg_received = _socket.receive_msg();
        if (msg_received.equals("machina")) {
            System.out.println("get machina. cool!");
        } else {
            System.out.println("hand shake failed.");
            _socket.disconnect();
            return "hand shake failed.";
        }

        // send ur real need
		_socket.send_msg("list all files");
        msg_received = _socket.receive_msg();
    } catch (Exception e) {
        System.out.println("oh boy, don't know where to start it." + "\n");
        return "oh boy, don't know where to start it.";
    }
    return msg_received;
}

// return value: 0 - successful; 1 - transfer ok, but file IO error; 2 - transfer or connection failed;
//				3 - file does not exist in the server
public int receive_data(String filename) {
    _filesize = -1;
	if (_socket == null) {
		System.out.println("no socket");
		return 2;
	}

    DataInputStream dataInputStream = null;
    BufferedInputStream inputStream = null;
	try {
		// retreve the stream
        dataInputStream = _socket.getInputStream();
		inputStream = new BufferedInputStream (new BufferedInputStream(dataInputStream));
		
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
//	     fullname = _savedir + fullname;
        fullname = _ctx.getFilesDir() + "/" + fullname;

	     // receive part2, which is file size. 
	     long filesize = Integer.parseInt(_socket.receive_msg());
        _socket.send_msg("ok");
	     System.out.println("file length: " + filesize + "\n");

	     // 用于接收二进制文件流的stream, get ready for receiving the third part, which is the binary content
	     DataOutputStream fileStream = null;
	     try {

             FileOutputStream fileoutputStream = new FileOutputStream(fullname);
//             FileOutputStream fileoutputStream = _ctx.openFileOutput(fullname, _ctx.MODE_PRIVATE);
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
	     int bufferSize = 32768;
	     byte[] receive_buff = new byte[bufferSize];
	     // start to receive
	     while (true) {
	    	 int receive_current = 0;
	    	 if (inputStream != null) {
	    		 // beware that buffsize. only receive that amount of data each time
                 try {
                     receive_current = inputStream.read(receive_buff);
                 } catch (Exception ee) {
                     inputStream.close();
                     return 2;
                 }
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
        _filesize = receive_total;

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

static public class GetFile  implements Runnable {
    static private Context _ctx = null;
    static private String _filename = "";
    static private String _host = "";
    static private int _port = -1;
    static private int _filesize = -1;
    static private int _success = -1;
    static private boolean _ok = false;

    public GetFile(Context ctx, String filename, String host, int port) {
        _ok = false;
        _filename = filename;
        _host = host;
        _port = port;
        _ctx = ctx;
    }

    static public boolean is_ok() {
        return _ok;
    }

    static public int get_success() {
        return _success;
    }

    static public int get_filesize() {
        return _filesize;
    }

    static public String get_filename() {
        return _filename;
    }

    @Override
    public void run() {
        _ok = false;
        _filesize = -1;
        _success = -1;
        FileTransfer ft = new FileTransfer(_filename, _ctx);
        try {
            ft.connect(_host, _port, 5000);
        } catch (Exception e) {
            ft.disconnect();
            _ok = true;
            return;
        }

        _success = ft.receive_data(_filename);
        _filesize = ft._filesize;
        ft.disconnect();
        _ok = true;
    }
}


    static public class GetFileList  implements Runnable {
        static private String _filelist = "";
        static private String _host = "";
        static private int _port = -1;
        static private boolean _ok = false;

        public GetFileList(String host, int port) {
            _ok = false;
            _host = host;
            _port = port;
        }

        static public boolean is_ok() {
            return _ok;
        }

        static public String get_filelist() {
            return _filelist;
        }

        @Override
        public void run() {
            _ok = false;
            _filelist = _host + "_" + String.valueOf(_port) + " connection error.";
            FileTransfer ft = new FileTransfer("", null);
            try {
                ft.connect(_host, _port, 5000);
            } catch (Exception e) {
                ft.disconnect();
                _ok = true;
                return;
            }

            _filelist = ft.get_filelist();
            ft.disconnect();
            _ok = true;
        }
    }

}
