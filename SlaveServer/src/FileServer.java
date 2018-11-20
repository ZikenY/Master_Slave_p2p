import java.io.*;
import java.net.*;
import java.util.*;

public class FileServer {
	static private String _dir = "";
	static private int _fileserver_connection_count = 0;
	static class FileTransfer implements Runnable {
		public static Boolean _running = true;
		private Socket _socket = null;
		private String _client_ip = "uninitialized host";
		private int _client_port = -1;

		public FileTransfer(Socket socket) {  
			_socket = socket;
			_client_ip = get_clean_ip(_socket.getRemoteSocketAddress().toString());
			// change localhost to the real ip address
			if (_client_ip.equals("127.0.0.1") || _client_ip.equals("localhost")) {
				// change localhost to the real ip address
				try {
					_client_ip = InetAddress.getLocalHost().getHostAddress();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			}			

			_client_port = get_clean_port(_socket.getRemoteSocketAddress().toString());
			System.out.println(" --new client came up: " + _client_ip + ":" + _client_port);
		}
		
	    private String get_clean_ip(String host) {
			String ss[] = host.substring(1).split(":");
			return ss[0];
		}

		private int get_clean_port(String host) {
			String ss[] = host.substring(1).split(":");
			return Integer.parseInt(ss[1]);
		}

		public void run() {
			try {
				_fileserver_connection_count++;
				deal_with_socket();
			} catch (Exception e) {  
				e.printStackTrace();  
			}
			
			_fileserver_connection_count--;
			System.out.println("client socket closed: " + _client_ip);
			// this connection has been closed
		}  
        
		// deal with the real file transfer
		private void deal_with_socket() throws Exception {  
		    char msg_buff[] = new char[16384];
		    Arrays.fill(msg_buff, '0');
		    String filename = "";
		    
		    // 首先从client shake hands, then get request
			try {
	            Reader reader = new InputStreamReader(_socket.getInputStream());
                Writer writer = new OutputStreamWriter(_socket.getOutputStream());
	            // check client
	            int receive_size = reader.read(msg_buff);
	            if (receive_size > 0)
	            {
	                String client_msg = new String(msg_buff, 0, receive_size);
	                System.out.println("first contact: " + client_msg);
	                if (!client_msg.equals("i love ex machina"))
	                {
						System.out.println("\nhand shake failed. bad client request.");
						writer.close();
						reader.close();
						_socket.close();
						return;
	                }
	                
	                writer.write("machina");
	                writer.flush();
	                System.out.println("machina to client...");

	                // now client has passed the hand shake. what is ur real request?
	                receive_size = reader.read(msg_buff);
	                client_msg = new String(msg_buff, 0, receive_size);
	                System.out.println("client's request: " + client_msg);
	                if (client_msg.equals("cancel")) {
	                	writer.write("crap%$!#");
	                	writer.flush();
						writer.close();
	                	reader.close();
	                	_socket.close();
	                	return;
	                } else if (client_msg.equals("i love cats")) {
	                	writer.write("miaow");	// kidding
	                	writer.flush();
	                } else if (client_msg.equals("list all files")) {
	                	String filelist = getFileList();
	                	writer.write(filelist);
	                	writer.flush();
	                	System.out.println("file list sent to the client.");
						writer.close();
	                	reader.close();
	                	_socket.close();
	                	return;
	                } else if (client_msg.equals("need a file")) {
	                	writer.write("what do u need?");
	                	writer.flush();
	                	System.out.println("to client: which file do u need?");
	                	// proceed the rest codes
	                }
	            }

	            // get filename from client
	            receive_size = reader.read(msg_buff);
	            if (receive_size > 0) {
	            	String tmp = new String(msg_buff, 0, receive_size);
	            	filename = _dir + tmp;
	                System.out.println("filename required: " + filename);
	            } else {
					_socket.close();				
					System.out.println("\nclient after shake hand ladida!");
					return;
	            }
			} catch (IOException e1) {
				_socket.close();				
				System.out.println("\ndummyInputStream ladida!");
				return;
			}			        	
			
			// 接下来正式干活
			// 打开文件，File类用于取得文件长度和文件名
		    File dataFile = new File(filename);
		    if (!dataFile.exists()) {
		        DataOutputStream outputStream = new DataOutputStream(_socket.getOutputStream());		        
		        outputStream.writeUTF("i don't have a file named " + filename);
		        outputStream.flush();
		    	_socket.close();	
		    	System.out.println("have any idea where the file is?: " + filename);
		    	return;
		    }

		    DataInputStream fileInputStream;
			try {
				fileInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)));
		
		    	System.out.println("filename: " + filename);
		    	System.out.println("file size: " + dataFile.length());
		    	System.out.println("client at port: " + _socket.getPort() + "...");
			    
			    try{
			        DataOutputStream dataOutputStream = new DataOutputStream(_socket.getOutputStream());
			        BufferedOutputStream outputStream = new BufferedOutputStream(new BufferedOutputStream(dataOutputStream));
			        
			        Reader reader = new InputStreamReader(_socket.getInputStream());
	                Writer writer = new OutputStreamWriter(_socket.getOutputStream());

	                // send the filename
	                writer.write(dataFile.getName());
	                writer.flush();
	                reader.read(msg_buff);

			        // 传输文件长度
			        writer.write(String.valueOf(dataFile.length()));
	                writer.flush();
	                reader.read(msg_buff);
			        
			        // 读取文件内容，一次xxxx个字节传输给client
			        int bufferSize = 16384;
			        int bytecount = 0;
			        byte[] data_buff = new byte[bufferSize];
			        while (bytecount < dataFile.length()) {
			        	int read_size = 0;
			        	if (fileInputStream != null) {
			        		// 每次读取长度最多为bufferSize的内容
			        		read_size = fileInputStream.read(data_buff);
			        		bytecount += read_size;
			        	}
			        	
			        	if (read_size == -1) {
			        		// file io error
			        		_socket.close();
							fileInputStream.close();	
			        		break;
			        	}
			        	
			        	// 将这次读取的block传送给客户端
			        	outputStream.write(data_buff, 0, read_size);
			        }
			        outputStream.flush();
			        System.out.println("\nfile sized of " + bytecount + " bytes transfered.");
			    } catch (Exception e){
			    	_socket.close();
					fileInputStream.close();
			    	System.out.println("outputStream ladida!");
			    	return;
			    }
			    
			    // don't forget close the socket & stream
			    _socket.close();    
				fileInputStream.close();
			} catch (IOException e2) {
		    	System.out.println("file sucks!\n");
				return;
			}
		    
		}  
	}

	static class DirectoryServerBirdge extends Thread{
		private String _host = "uninitialized host";
		private int _port = -1; 
		private int _listening_port;
        char _buff[] = null;
		boolean _running = true;
        
		public DirectoryServerBirdge(String host, int port, int listening_port) throws Exception {
			_host = host;
			_port = port; 
			_listening_port = listening_port;
		}
		
		public void run() {
			String filelist = getFileList();
			Set<String> original_set = FileList2Set(filelist);
			Socket socket = null;
			Writer writer = null;
			Reader reader = null;
			_buff = new char[65536];
			Arrays.fill(_buff, '0');
			boolean sendwhole = true;
			
			while (_running) {
				try {
					int timeout_retry_time = 5000;
					int timeouttime = timeout_retry_time;
					while (timeouttime-- != 0) {
						try {
							socket = new Socket();
					        InetSocketAddress isa = new InetSocketAddress(_host, _port);
					        socket.connect(isa, 2000);
						} catch (IOException e) {
							System.out.println("connecting to directory server is timeout. try again. " + timeouttime + " times left");
							continue;
						}
						
						break;
					}

					if (!socket.isConnected()) {
						System.out.println("cannot connect to directory server");
						return;
					} else if (timeouttime < timeout_retry_time-1){
						System.out.println(" --reconnected to directory server: " + _host);
					}
					
					_host = socket.getInetAddress().getHostAddress();
					
					writer = new OutputStreamWriter(socket.getOutputStream());
					reader = new InputStreamReader(socket.getInputStream());
	
					// first thing is first: hands shaking
					writer.write("i am a fileserver");
					writer.flush();
					if (!check_acknowledge(socket, reader)) {
						return;
					}
	
					// 2nd step, send listening port for file server 
					writer.write(String.valueOf(_listening_port));
					writer.flush();
					if (!check_acknowledge(socket, reader)) {
						return;
					}
					
					if (sendwhole) {
						sendwhole = false;						
						writer.write("come first time");
					} else {
						writer.write("hey buddy");
					}
					writer.flush();
					
					// at this time the dir server either ask for file list or simply return a ack 
					int receive_bytes = reader.read(_buff);
					String msg = new String(_buff, 0, receive_bytes);
					if (msg.equals("need whole filelist")) {
						// establish a file list-map and send the whole list to the directory_Server
						writer.write(filelist);
						writer.flush();
						check_acknowledge(socket, reader);
						System.out.println(" --connected to directory server: " + _host);
						
					} else {
						// dir server already has the filelist. just send the update info
						String newfilelist = "";
						String removedfilelist = "";
						
						String updatedfilelist = getFileList();	// get new list every second
						String newfilelistarray[] = updatedfilelist.split("\n");
						
						// check if there is a new file 
						for (int i=0; i<newfilelistarray.length; i++)
						{
							if (!original_set.contains(newfilelistarray[i])) {
								// there is a new file
								if (newfilelist == "") {
									newfilelist = newfilelistarray[i];
								} else {
									newfilelist = newfilelist + "\n" + newfilelistarray[i];
								}
								
								// don't forget to update the original set
								original_set.add(newfilelistarray[i]);
							}
						}
						
						// check if there is a file removed
						Set<String> updated_set = FileList2Set(updatedfilelist);
						Iterator<String> it_ori = original_set.iterator();
						while (it_ori.hasNext())	{// 这个时候要遍历原始set，看是否在新set中不存在了
							String filename = it_ori.next();
							if (!updated_set.contains(filename)) {
								// this one has been removed
								if (removedfilelist == "") {
									removedfilelist = filename;
								} else {
									removedfilelist = removedfilelist + "\n" + filename;
								}
							}
						}
	
						// don't forget to update the original set
						if (!removedfilelist.equals("")) {
							String removearray[] = removedfilelist.split("\n");
							for (int i=0; i<removearray.length; i++)
							{
								original_set.remove(removearray[i]);
							}
						}
						
						// send the request of sending new file list to directory server
						if (!newfilelist.equals("")) {
							System.out.println("-- new file: \n" + newfilelist);
							writer.write("update: new file list");
							writer.flush();
							check_acknowledge(socket, reader);
							
							// send the new file list to directory server
							writer.write(newfilelist);
							writer.flush();
							check_acknowledge(socket, reader);
						}
						
						// send the request of sending new removed file list to directory server
						if (!removedfilelist.equals("") && !removedfilelist.equals("\0")) {
							System.out.println("-- remove file: \n" + removedfilelist);
							writer.write("update: removed file list");
							writer.flush();
							check_acknowledge(socket, reader);
							
							// send the removed file list to directory server
							writer.write(removedfilelist);
							writer.flush();
							check_acknowledge(socket, reader);
						}
						
						// just check in, then send current fileserver link count
						writer.write("just check in");
						writer.flush();
						check_acknowledge(socket, reader);
						writer.write(String.valueOf(_fileserver_connection_count));
						writer.flush();
						check_acknowledge(socket, reader);
					}

					writer.write("close socket");
					writer.flush();
					// no need to check ack
					socket.close();
					Thread.sleep(1000);
//					System.out.println("just checked in");

				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("IO error!");
					return;
				} catch (InterruptedException e) {
					e.printStackTrace();
					System.out.println("thread error!");
					return;
				}
			}
			
			// notice that at this point the socket should be still active
			// unregister from directory server
			if (socket != null && socket.isConnected()) {
				try {
					writer.write("fileserver unregister");
					// don't need ack from dir server now
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
				
				System.out.println("unregister fileserver: " + _host + ":" + _listening_port);
				return;
			}
		}

		private boolean check_acknowledge(Socket socket, Reader reader) throws IOException {
			try {
				int receive_bytes = reader.read(_buff);
	            if (receive_bytes <= 0) {
	                return false;
	            }
	    
				String msg = new String(_buff, 0, receive_bytes);
				if (!msg.equals("acknowledge"))
				{
					socket.close();				
					System.out.println("unmitigated acknowledge error from " + _host);
					System.out.println("connection to " + _host + " has been terminated:(");
					return false;
				}
			} catch (Exception e) {
				System.out.println("unmitigated acknowledge error from " + _host);
				System.out.println("connection to " + _host + " has been terminated:(");
				System.out.println("maybe connection reset!");
				return false;
			}
			return true;
		}

	}


	   public static String getLocalIP() {
	       String ip = "";
	       try {
	           Enumeration<?> e1 = (Enumeration<?>) NetworkInterface.getNetworkInterfaces();
	           while (e1.hasMoreElements()) {
	               NetworkInterface ni = (NetworkInterface) e1.nextElement();
//	               if (!ni.getName().equals("en0") && !ni.getName().equals("eth0")) {
	               if (false) {
//	            	   System.out.println("ni.getName()" + ni.getName());
//	                   continue;
	               } else {
	            	   String tmp = "";
	                   Enumeration<?> e2 = ni.getInetAddresses();
	                   while (e2.hasMoreElements()) {
	                       InetAddress ia = (InetAddress) e2.nextElement();
	                       if (ia instanceof Inet6Address)
	                           continue;
	                       tmp = ia.getHostAddress();
	                       break;
	                   }
	                   
	                   if (!tmp.equals("127.0.0.1") && !tmp.equals(ip) && !tmp.equals("")) {
	                	   ip = tmp;
	                	   System.out.println(ni.getName() + ": " + ip);
	                   }
//	                   break;
	               }
	           }
	       } catch (SocketException e) {
	           e.printStackTrace();
	           System.exit(-1);
	       }
	       
	       return ip;
	   }

	   public static String byteHEX(byte ib) {
	       char[] Digit = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a','b', 'c', 'd', 'e', 'f' };
	       char[] ob = new char[2];
	       ob[0] = Digit[(ib >>> 4) & 0X0F];
	       ob[1] = Digit[ib & 0X0F];
	       String s = new String(ob);
	       return s;
	   }
	
	// arg0: file directory
	// arg1: listening port
	// arg2: directory server ip
	// arg3: directory server port
	public static void main(String args[]) throws Exception {
	/*
    	args = new String[4];
		args[0] = "D:\\foto\\wallpapers\\"; 
		args[1] = "8821"; 
		args[2] = "127.0.0.1"; 
		args[3] = "8911"; 
*/		
		_dir = "/media/ziken/stuff/download/pic";
//		_dir = "//Users//violaine//Documents//study/513//";	
		if (args.length >= 1) {
			_dir = args[0];
		}

		int listening_port = 8821;
		if (args.length >= 2) {
			listening_port = Integer.parseInt(args[1]);
		}

		// server for listening
		ServerSocket serverSocket = new ServerSocket(listening_port);

		// create the bridge to directory server
		if (args.length >= 3) {
			String directoryServerHost = args[2];
			int directoryServerPort = 8911;
			if (args.length >= 4) {
				directoryServerPort = Integer.parseInt(args[3]); 
			}

			DirectoryServerBirdge bridge = new DirectoryServerBirdge(directoryServerHost, directoryServerPort, listening_port);
			bridge.start();
			
	    	System.out.println("directory server is at " + directoryServerHost + ":" + directoryServerPort);
		}
		
	    Thread.sleep(300);
		// listening for the file transferring
		while (FileTransfer._running) {
	    	System.out.println("shared directory: " + _dir);
	    	System.out.println("host IP must be one of the follow(s): ");	getLocalIP();
	    	System.out.println("listening at: " + listening_port + "...");			
			Socket socket = serverSocket.accept();
			new Thread(new FileTransfer(socket)).start();  
		}

		serverSocket.close();
	}
	
	static public String getFileList()
	{
		String filelist = "";
		try {
			File file = new File(_dir);
			File[] files = file.listFiles();
			for (int i = 0; i < files.length; i++) {
				if(files[i].isDirectory()){
					continue;
				}
				
				String filename = files[i].getName();
				if (filelist == ""){
					filelist = filename;
				} else {
					filelist = filelist + "\n" + filename;
				}
			}
		} catch (Exception e) {
			System.out.println("incorrected directory: " + _dir);
		}
		
		if (filelist.equals("")) {
			System.out.println("empty directory: " + _dir);
			return "\0";
		}

        try {
			filelist = new String(filelist.getBytes("GBK"), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return filelist;
	}
	
	static public Set FileList2Set(String filelist)
	{
		String stringarray[] = filelist.split("\n");
		Set set = new HashSet();
		for (int i=0; i<stringarray.length; i++)
		{
			set.add(stringarray[i]);
		}

		return set;
	}
	
	
}
