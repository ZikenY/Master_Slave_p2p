import java.io.*;
import java.util.Arrays;

public class FileRequester extends ClientSocketEx{
private String _dir = "undefined";
private String _fs_desc = "undefined";

public FileRequester(String dir) {
	_dir = dir;
}

public void disconnect() {
	// say good-bye to dir server 
	if (_socket == null || !_socket.isconnected()) {
		return;
	}
	
	try {
		_socket.send_msg("filerequester disconnect");
	} catch (Exception e) {
		e.printStackTrace();
	}	
	super.disconnect();
}

private boolean shakehand() {
	if (_socket == null) {
		System.out.println("no socket");
		return false;
	}

	try {
		// 1st, shake hand
		_socket.send_msg("i am a filerequester", false);
		String msg = _socket.receive_msg();
		if (!msg.equals("acknowledge")) {
			System.out.println("faulty directory server");
			return false;
		}

	} catch (Exception e) {
		e.printStackTrace();
	}
	
	return true;
}

private String require_filelist() {
	if (!this.shakehand()) {
		return "";
	}

	String msg = "";
	try {
		// 2nd, require filelist
		_socket.send_msg("require filelist", false);
		msg = _socket.receive_msg();
	} catch (Exception e) {
		e.printStackTrace();
	}

	if (msg.equals("\0")) {
		msg = "";
	}
		
	return msg;
}

private String require_file(String filename) {
	if (!this.shakehand()) {
		return "";
	}

	String msg = "";
	try {
		// 2nd, require a file
		_socket.send_msg("require a file", false);
		_socket.receive_msg();
		_socket.send_msg(filename, false);
		// the received msg format mush be host_ip +\n+ port of a fileserver
		msg = _socket.receive_msg();
	} catch (Exception e) {
		e.printStackTrace();
	}
	
	if (msg.equals("\0")) {
		msg = "";
	}
	
	return msg;
}

private void report_status(String msg) {
	if (!this.shakehand()) {
		return;
	}

	try {
		_socket.send_msg(msg, false);
		_socket.receive_msg();
		_socket.send_msg(_fs_desc, false);
		_socket.receive_msg();		
	} catch (Exception e) {
		e.printStackTrace();
	}
}

private boolean call_filetransfer(String host, int port, String filename) {
	_fs_desc = host + "_" + String.valueOf(port);
	// call the filetransfer
	FileTransfer fileTransfer = new FileTransfer(_dir);
	try {
		if (fileTransfer.connect(host, port, 3000, "file server")) {
			int r = fileTransfer.receive_data(filename); 
			if (r == 0) {
				this.report_status("fileserver transfer succeeded");
			} else if (r == 2){
				this.report_status("fileserver transfer failed");
			}
			
			fileTransfer.disconnect();
			return true;
		} else {
			this.report_status("fileserver connect error");
		}
	} catch (Exception ex) {
		ex.printStackTrace();
		System.out.println("cannot get file from fileserver");
		this.report_status("fileserver transfer failed");
	}
	
	fileTransfer = null;
	return false;
}

public static void main(String args[]) {
/*
	args = new String[4];
	args[0] = "localhost"; 		// directory server host
	args[1] = "8911";			// directory server port
//	args[2] = "卷筒猫.jpg"; 		// filename
	args[2] = "_DSC7688.jpg"; 	// filename
//	args[2] = "P17050_817699610.jpg";
	args[3] = "c:\\Users\\ziken\\Desktop\\"; 			// local directory
//	args[3] = "/Users/violaine/Desktop/"; 			// local directory
*/
	
	String dirsvr_host = args[0];
	int dirsvr_port = Integer.parseInt(args[1]);
	String filename = args[2];
	String local_dir = args[3];
	try {
		String encoding = System.getProperty("file.encoding");  
		local_dir = new String(local_dir.getBytes(encoding), encoding);
	} catch (UnsupportedEncodingException e) {
		e.printStackTrace();
		return;
	}
	
	FileRequester fileRequester = new FileRequester(local_dir);

	if (fileRequester.connect(dirsvr_host, dirsvr_port, 3000, "directory server")) {
/*
		String filelist = fileRequester.require_filelist();
		System.out.println(filelist);
		if (filelist.equals("")) {
//			fileRequester.disconnect();
//			return;
			System.out.println("no file at all.");
		}
*/
		
		String fs_disc = fileRequester.require_file(filename);
		if (fs_disc.equals("")) {
			fileRequester.disconnect();
			System.out.println("no such file shared: " + filename);
			return;
		}
		
		System.out.println(fs_disc);
		String[] ss = fs_disc.split("\n");
		if (fileRequester.call_filetransfer(ss[0], Integer.parseInt(ss[1]), filename)) {
			System.out.println("done.");
		}
		
		fileRequester.disconnect();
	}
}

}
