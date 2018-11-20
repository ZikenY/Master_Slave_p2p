import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;


public class DirectoryServer {
	static class FileServerDescription {
		public FileServerDescription(String host_ip, int listening_port) {
			_host_ip = host_ip;
			_listening_port = listening_port;
			_current_connection_count = 0;
			_success_count = 0;
			_fail_count = 0;			
        	_last_connect_tick = System.currentTimeMillis();

		}

		public String _host_ip;
		public int _listening_port;
		public int _current_connection_count;	// the number of client connected to the fileserver currently
		public int _success_count;				// the number of successful file transfer
		public int _fail_count;					// the number of fail file transfer
		public int _connect_fail_count;			// the number of fail fileserver connection error
		public double _last_connect_tick;	    // for timeout cleaning
	}

	// for synchonizatoin
	static private FileServerDescription hash_sym = new FileServerDescription("", 0);
	
	// store all fileservers' descriptions
    // key: ip+ "_" + port
	static Hashtable<String, FileServerDescription> _fsd_table = new Hashtable<String, FileServerDescription>();
	
	// store all the files
	// key: filename; value: a set of fsd keys
	static Hashtable<String, Set<String>> _file_table = new Hashtable<String, Set<String>>();

	// remove fileserver registation from hashtables
	static private void remove_fileserver(String fsd_key) {
		// 用于存放需要更新的Set<String> fsd列表 in file table
		Hashtable<String, Set<String>> filename_update = new Hashtable<String, Set<String>>();
		
		// 先遍历file table
    	Iterator<Entry<String, Set<String>>> it_ft = _file_table.entrySet().iterator();
    	while (it_ft.hasNext()) {
    		Entry<String, Set<String>> entry = it_ft.next();
    		// 去掉这个set中的fsd key，稍后重新更新file_map
    		Set<String> fsdkeys = entry.getValue();
    		fsdkeys.remove(fsd_key);
    		// 注意这里还没有更新到file_map
    		filename_update.put(entry.getKey(), fsdkeys);
    	}
    	
    	Iterator<Entry<String, Set<String>>> it_update = filename_update.entrySet().iterator();
    	while (it_update.hasNext()) {
    		Entry<String, Set<String>> entry = it_update.next();
    		Set<String> fsdkeys = entry.getValue();
    		// 先从file table中删除
			_file_table.remove(entry.getKey());
    		if (fsdkeys.size() > 0) {
    			// update the file table
    			_file_table.put(entry.getKey(), fsdkeys);
    		} else {
    			// 木有文件了, remove the whole entry from file table
    			System.out.println("file no long existed: " + entry.getKey());
    		}
    	}
    	
    	// then, remove the fileserver from the _fsd_table
    	_fsd_table.remove(fsd_key);

	}
	
	static class DirServer implements Runnable {
		static boolean _running = true;
		Socket _socket = null;
		String _remote_ip = "uninitialized host";
		int _remote_port = -1;
		int _fileserver_listen_port = -1;
	    char _msg_buff[] = new char[32768];
	    
	    // 0: fileserver; 1: filerequester; 2: monitor
	    int _client_type = -1;
	    
	    public DirServer(Socket socket) {
			_socket = socket;
			_remote_ip = get_clean_ip(_socket.getRemoteSocketAddress().toString());
			if (_remote_ip.equals("127.0.0.1") || _remote_ip.equals("localhost")) {
				// change localhost to the real ip address
				try {
					_remote_ip = InetAddress.getLocalHost().getHostAddress();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			}			
			_remote_port = get_clean_port(_socket.getRemoteSocketAddress().toString());
		}

	    private String get_clean_ip(String host) {
			String ss[] = host.substring(1).split(":");
			return ss[0];
		}

		private int get_clean_port(String host) {
			String ss[] = host.substring(1).split(":");
			return Integer.parseInt(ss[1]);
		}

		public void responseFileServer(Reader reader, Writer writer) {
			int receive_size;
        	try {
        		// should be fileserver's listening port
            	receive_size = reader.read(_msg_buff);
            	_fileserver_listen_port = Integer.parseInt(new String(_msg_buff, 0, receive_size));
            	send_acknowledge(writer);

            	receive_size = reader.read(_msg_buff);
            	String msg_received =new String(_msg_buff, 0, receive_size);

            	synchronized(hash_sym) {
	            	// check if this fileserver has already been registered.
	            	if (_fsd_table.containsKey(get_fsd_key()) && !msg_received.equals("come first time")) {
	            		// already
	                	send_acknowledge(writer);
	                	// update the _last_connect_tick
	                	FileServerDescription fsd = _fsd_table.get(get_fsd_key());
	                	fsd._last_connect_tick = System.currentTimeMillis();
	                	_fsd_table.put(get_fsd_key(), fsd);
	                	
	            	} else {            	
	            		// a new fileserver or just come first time
	            		if (_fsd_table.containsKey(get_fsd_key())) {
		        			remove_fileserver(get_fsd_key());
	            		}
	            		
		    			// put new fileserver description into the hashtable
		    			FileServerDescription fsd = new FileServerDescription(_remote_ip, _fileserver_listen_port);
		    			_fsd_table.put(get_fsd_key(), fsd);
	
		    			// ask for the fileserver to send filelist
		    			writer.write("need whole filelist");
		    			writer.flush();
		            	// get file list here	            	
			            receive_size = reader.read(_msg_buff);
			            String filelist = new String(_msg_buff, 0, receive_size);
		            	send_acknowledge(writer);
		            	// maintain the file table
		            	batch_update_file_table(filelist, this.get_fsd_key());
		            	
		    			System.out.println(" --register fileserver: " + this.get_fsd_key());
	            	}
            	}
	            	
            	while (_socket.isConnected()) {            		
	        		// just get current fileserver update
	            	receive_size = reader.read(_msg_buff);
		            msg_received = new String(_msg_buff, 0, receive_size);
	    			
		            if (msg_received.equals("close socket")) {
		            	// no need to ack
		            	_socket.close();
		            	break;
		            	
            		} else if (msg_received.equals("just check in")) {
		            	send_acknowledge(writer);
			            receive_size = reader.read(_msg_buff);
		            	send_acknowledge(writer);
		            	// maintain the fileserver table
			            int fs_connect_count = Integer.parseInt(new String(_msg_buff, 0, receive_size));
			            update_fs_connect_count(this.get_fsd_key(), fs_connect_count);
	
		            } else if (msg_received.equals("update: new file list")) {
		            	send_acknowledge(writer);
	 
		            	// get new file list here	            	
			            receive_size = reader.read(_msg_buff);
			            String newfilelist = new String(_msg_buff, 0, receive_size);
		            	send_acknowledge(writer);
		            	System.out.println(" --new files from "
		            	  + _remote_ip + ":" + _remote_port + "...\n" + newfilelist);
	
		            	// maintain the file table
		            	batch_update_file_table(newfilelist, this.get_fsd_key());
		            	
		            } else if (msg_received.equals("update: removed file list")) {
		            	send_acknowledge(writer);
	
		            	// get removed file list here	            	
			            receive_size = reader.read(_msg_buff);
			            String removedfilelist = new String(_msg_buff, 0, receive_size);
		            	send_acknowledge(writer);
	
		            	batch_remove_file_from_table(removedfilelist, this.get_fsd_key());
		            	
		            } else if (msg_received.equals("fileserver unregister")) {
		            	// don't need to send ack
		            	_socket.close();
		            	// remove this one from hashtables
	    				remove_fileserver(get_fsd_key());
	    				System.out.println(" --unregister fileserver: " + this.get_fsd_key());	    				
		            } else {
//						System.out.println("-- from fileserver " + get_fsd_key() + ": " + msg_received);
		            	send_acknowledge(writer);
		            }
            	}
			} catch (IOException e) {
				System.out.println("fileserver socket IOException: " + get_fsd_key());
			}
		}
				
		public void responseFileRequester(String msg_received, Reader reader, Writer writer) {
        	try {
	            if (msg_received.equals("require filelist")) {
	            	send_filelist(writer);
	            	
	            } else if (msg_received.equals("require a file")) {
	            	send_acknowledge(writer);	//then the requester will send a filename

	            	// need to send a fileserver host_ip +\n+ port to the requester
	            	int receive_size = reader.read(_msg_buff);
		            String filename = new String(_msg_buff, 0, receive_size);
		            send_fileserver_info(writer, filename);
		            
	            } else if (msg_received.equals("fileserver connect error")){
	            	send_acknowledge(writer);
	            	//then the requester will send the fileserver fsd (ip_port)
	            	int receive_size = reader.read(_msg_buff);
	            	send_acknowledge(writer);
		            String fsd_key = new String(_msg_buff, 0, receive_size);
		            FileServerDescription fsd = _fsd_table.get(fsd_key);
		            if (fsd != null) {
		            	fsd._connect_fail_count++;
		            	_fsd_table.remove(fsd_key);
		            	_fsd_table.put(fsd_key, fsd);
		            }		            
		            System.out.println("(reported from filerequester) fileserver connect error: " + fsd_key);
		            
	            } else if (msg_received.equals("fileserver transfer succeeded")){
	            	send_acknowledge(writer);
	            	//then the requester will send the fileserver fsd (ip_port)
	            	int receive_size = reader.read(_msg_buff);
	            	send_acknowledge(writer);
		            String fsd_key = new String(_msg_buff, 0, receive_size);
		            FileServerDescription fsd = _fsd_table.get(fsd_key);
		            if (fsd != null) {
		            	fsd._success_count++;
		            	_fsd_table.remove(fsd_key);
		            	_fsd_table.put(fsd_key, fsd);
		            }
		            System.out.println("(reported from filerequester) fileserver transfer succeeded: " + fsd_key);

		            
	            } else if (msg_received.equals("fileserver transfer failed")){
	            	send_acknowledge(writer);
	            	//then the requester will send the fileserver fsd (ip_port)
	            	int receive_size = reader.read(_msg_buff);
	            	send_acknowledge(writer);
		            String fsd_key = new String(_msg_buff, 0, receive_size);
		            FileServerDescription fsd = _fsd_table.get(fsd_key);
		            if (fsd != null) {
		            	fsd._fail_count++;
		            	_fsd_table.remove(fsd_key);
		            	_fsd_table.put(fsd_key, fsd);
		            }
		            System.out.println("(reported from filerequester) fileserver transfer failed: " + fsd_key);

	            } else if (msg_received.equals("filerequester disconnect")) {
	            	// no need to send ack
	            	_socket.close();
	            	System.out.println("filerequester disconnect");
	            	
	            } else {
					System.out.println("-- from FileRequester -" + _remote_ip + ":" + _remote_port + "- :" + msg_received);
	            	send_acknowledge(writer);
	            }
			} catch (IOException e) {
				System.out.println("socket error!");
				e.printStackTrace();
			}
        }
		
		public void goMonitor(Reader reader, Writer writer) {
    		System.out.println("new monitor connected at: " + _remote_ip + ":" + _remote_port);
        	try {
        		while (_socket.isConnected()) {
	            	int receive_size = reader.read(_msg_buff);
	            	if (receive_size < 0) {
	            		break;
	            	}	            	
		            String msg = new String(_msg_buff, 0, receive_size);
		            
		            if (msg.equals("i am a monitor")) {
		            	send_acknowledge(writer);

        			} else if (msg.equals("request file manifests")) {
		            	String filemanifest = get_file_manifest();
		            	// first send the message size
		            	filemanifest = new String(filemanifest.getBytes("GBK"), "UTF-8");
		    			writer.write(String.valueOf(filemanifest.length()));
		    			writer.flush();		    			
		    			reader.read(_msg_buff);	// just receive the respond
		    			// send mass data
		    			int from = 0;
		    			int to = 0;
		    			while (to < filemanifest.length())
		    			{
		    				if (to+1001 < filemanifest.length()) {
		    					to = from + 1001;
		    				} else {
		    					to = filemanifest.length();
		    				}
		    				
			    			writer.write(filemanifest.substring(from, to));
			    			writer.flush();
			    			reader.read(_msg_buff);	// just receive the respond
			    			from = to;
		    			}
		    			
		            } else if (msg.equals("request fileserver status")) {
		            	String filemanifest = get_fileserver_status();
		    			writer.write(filemanifest);
		    			writer.flush();

		            } else if (msg.equals("disconnect")) {
		            	// no need to ack
		    			System.out.println("monitor left.");
		    			break;

		            } else if (msg.equals("fileserver connect error")){
		            	send_acknowledge(writer);
		            	//then the requester will send the fileserver fsd (ip_port)
		            	receive_size = reader.read(_msg_buff);
		            	send_acknowledge(writer);
			            String fsd_key = new String(_msg_buff, 0, receive_size);
			            FileServerDescription fsd = _fsd_table.get(fsd_key);
			            if (fsd != null) {
			            	fsd._connect_fail_count++;
			            	_fsd_table.remove(fsd_key);
			            	_fsd_table.put(fsd_key, fsd);
			            }		            
			            System.out.println("(reported from monitor) fileserver connect error: " + fsd_key);
			            
		            } else if (msg.equals("fileserver transfer succeeded")){
		            	send_acknowledge(writer);
		            	//then the requester will send the fileserver fsd (ip_port)
		            	receive_size = reader.read(_msg_buff);
		            	send_acknowledge(writer);
			            String fsd_key = new String(_msg_buff, 0, receive_size);
			            FileServerDescription fsd = _fsd_table.get(fsd_key);
			            if (fsd != null) {
			            	fsd._success_count++;
			            	_fsd_table.remove(fsd_key);
			            	_fsd_table.put(fsd_key, fsd);
			            }
			            System.out.println("(reported from monitor) fileserver transfer succeeded: " + fsd_key);

			            
		            } else if (msg.equals("fileserver transfer failed")){
		            	send_acknowledge(writer);
		            	//then the requester will send the fileserver fsd (ip_port)
		            	receive_size = reader.read(_msg_buff);
		            	send_acknowledge(writer);
			            String fsd_key = new String(_msg_buff, 0, receive_size);
			            FileServerDescription fsd = _fsd_table.get(fsd_key);
			            if (fsd != null) {
			            	fsd._fail_count++;
			            	_fsd_table.remove(fsd_key);
			            	_fsd_table.put(fsd_key, fsd);
			            }
			            System.out.println("(reported from monitor) fileserver transfer failed: " + fsd_key);
			            
		            } else {
		    			writer.write("i don't follow.");
		    			writer.flush();
		    			System.out.println("unexpected request from monitor: " + msg);
		            }
        		}
		
			} catch (IOException e) {
				System.out.println("socket error!");
				e.printStackTrace();
			}
        	
    		System.out.println("monitor disconnected at: " + _remote_ip + ":" + _remote_port);
		}
		
		// manifest item format: filename + "\n" + fsd count + "\n" + each fsd divided by "\n" 
		private String get_file_manifest() {
        	String filemanifest = "";
        	Iterator<Entry<String, Set<String>>> it = _file_table.entrySet().iterator();
        	while (it.hasNext()) {
        		Entry<String, Set<String>> entry = it.next();
        		if (filemanifest.equals("")) {
        			filemanifest = entry.getKey();
        		} else {
        			filemanifest = filemanifest + "\n" + entry.getKey();
        		}
        		
        		filemanifest += "\n";
        		
        		Set<String> fsd_set = entry.getValue();
        		filemanifest += String.valueOf(fsd_set.size()) + "\n";
        		boolean flag = false;
        		for (String fsd_key : fsd_set) {
        			if (flag) {
        				filemanifest += "\n";
        			} else {
        				flag = true;
        			}
        			filemanifest += fsd_key;        			
        		}
        	}

        	if (filemanifest.equals("")) {
        		filemanifest = "\0";
        	}
        	
        	return filemanifest;
		}
		
		private String get_fileserver_status() {
        	String svrstatus = "";
        	synchronized(hash_sym) {
	        	Iterator<Entry<String, FileServerDescription>> it = _fsd_table.entrySet().iterator();
	        	while (it.hasNext()) {
	        		Entry<String, FileServerDescription> entry = it.next();
	        		String fsd_key = entry.getKey();
	        		FileServerDescription fsd = entry.getValue();
	    			if (!svrstatus.equals("")) {
	    				svrstatus += "\n";
	    			}
	    			svrstatus = svrstatus + fsd_key + "\n";
	    			svrstatus = svrstatus + fsd._current_connection_count + "\n";
	    			svrstatus = svrstatus + fsd._success_count + "\n";
	    			svrstatus = svrstatus + fsd._fail_count + "\n";
	    			// this line does follow by "\n"
	    			svrstatus = svrstatus + fsd._connect_fail_count;
	        	}
        	}
        	
        	if (svrstatus.equals("")) {
        		svrstatus = "\0";
        	}
        	
        	return svrstatus;
		}
		
		private void send_acknowledge(Writer writer) throws IOException {
			writer.write("acknowledge");
			writer.flush();
		}
		
		public String get_fsd_key() {
			return _remote_ip + "_" + String.valueOf(_fileserver_listen_port);
		}		
		
		private void update_file_table(String filename, String fsd_key) {
			synchronized(hash_sym) {
				Set<String> fsds = null;
				if (_file_table.containsKey(filename)) {
					fsds = _file_table.get(filename);
					fsds.add(fsd_key);
				} else {
					fsds = new HashSet<String>();
					fsds.add(fsd_key);
				}
				
				_file_table.put(filename, fsds);
			}
        	System.out.println(" --update file" + filename + " from " + this.get_fsd_key());
		}
		
		private void batch_update_file_table(String filelist, String fsd_key) {
			if (filelist.equals("\0")) {
				System.out.println(" --empty server: " + this.get_fsd_key());
				return;
			}
			
			String filelistarray[] = filelist.split("\n");
			synchronized(hash_sym) {
				for (int i=0; i<filelistarray.length; i++) {
					update_file_table(filelistarray[i], fsd_key);
				}
			}
		}
		
		private void remove_file_from_table(String filename, String fsd_key) {
			synchronized(hash_sym) {
				if (_file_table.containsKey(filename)) {
					Set<String> fsds = _file_table.get(filename);
					if (fsds.contains(fsd_key)) {
						fsds.remove(fsd_key);
					}
					
					if (fsds.size() <= 0) {
						_file_table.remove(filename);
					}
	
	            	System.out.println(" --remove file" + filename + " from " + this.get_fsd_key());
				}
			}
		}

		private void batch_remove_file_from_table(String filelist, String fsd_key) {
			String filelistarray[] = filelist.split("\n");
			synchronized(hash_sym) {	
				for (int i=0; i<filelistarray.length; i++) {
					remove_file_from_table(filelistarray[i], fsd_key);
				}
			}
		}


		private void update_fs_connect_count(String fsd_key, int connect_count) {
			if (_fsd_table.containsKey(fsd_key)) {
				synchronized(hash_sym) {
					FileServerDescription fsd = _fsd_table.get(fsd_key);
					// is this ok? is fsd just a reference?
					fsd._current_connection_count = connect_count;
				}
			}
		}
		
		private void send_filelist(Writer writer) throws IOException {
        	String filelist = "";
        	Iterator<Entry<String, Set<String>>> it = _file_table.entrySet().iterator();
        	while (it.hasNext()) {
        		Entry<String, Set<String>> entry = it.next();
        		if (filelist.equals("")) {
        			filelist = entry.getKey();
        		} else {
        			filelist = filelist + "\n" + entry.getKey();
        		}
        	}
        	
        	if (filelist.equals("")) {
        		filelist = "\0";
        	}
        	
			writer.write(filelist);
			writer.flush();
		}
		
		private void send_fileserver_info(Writer writer, String filename) throws IOException {
			FileServerDescription fsd = null;
			// 先看有木有
			if (_file_table.containsKey(filename)) {
				Set<String> fsd_keys = _file_table.get(filename);
				Iterator<String> it_fsdkey = fsd_keys.iterator();
				while (it_fsdkey.hasNext()) {
					String fsd_key = it_fsdkey.next();
					FileServerDescription fsd_tmp =_fsd_table.get(fsd_key);
					if (fsd == null) {
						fsd = fsd_tmp;
					} else {
						// see which one has least fail count currently
						if (fsd_tmp._fail_count+fsd_tmp._connect_fail_count < fsd._fail_count+fsd._fail_count) {
							fsd = fsd_tmp;
						}
					}
				}
			}
			
			if (fsd != null) {
				String ip_port = fsd._host_ip + "\n" + fsd._listening_port;
				writer.write(ip_port);
				writer.flush();
			} else {
				// 木有
				writer.write("\0");
				writer.flush();
				System.out.println("no such file been found: " + filename);
			}
		}
		
		@Override
		public void run() {
			if (_socket == null || !_socket.isConnected()) {
				return;
			}

			Reader reader = null;
    		Writer writer = null;
			try {
				reader = new InputStreamReader(_socket.getInputStream());
				writer = new OutputStreamWriter(_socket.getOutputStream());
			
			    Arrays.fill(_msg_buff, '0');

			    int receive_size = reader.read(_msg_buff);
	            if (receive_size < 0) {
	            	System.out.println("socket error: " + _remote_ip + ":" + _remote_port);
	            	return;
	            }
	            String msg_received = new String(_msg_buff, 0, receive_size);
	            
                if (msg_received.equals("i am a fileserver")) {
	            	send_acknowledge(writer);
                	_client_type = 0;
                	
                } else if (msg_received.equals("i am a filerequester")) {
                	_client_type = 1;
	            	send_acknowledge(writer);
	            	
                } else if (msg_received.equals("i am a monitor")) {
                	_client_type = 3;
	            	send_acknowledge(writer);
	            	
                } else if (msg_received.equals("i love cats")) {
                	writer.write("miaow");	// kidding
                	writer.flush();
                	
                } else if (_client_type != 0 && _client_type != 1){
            		System.out.println(" --i don't know what it is :( ... " + msg_received);
                	writer.write("hey, i don't get it...");
                	writer.flush();
                }

            	if (_client_type == 0) {
            		responseFileServer(reader, writer);
            	}

            	if (_client_type == 1) {
                    while (_socket.isConnected()) {
        			    receive_size = reader.read(_msg_buff);
        	            if (receive_size < 0) {
        	            	System.out.println("socket error: " + _remote_ip + ":" + _remote_port);
        	            	return;
        	            }
        	            msg_received = new String(_msg_buff, 0, receive_size);
                    	responseFileRequester(msg_received, reader, writer);
                    }
            	}
                
            	if (_client_type == 3) {
            		goMonitor(reader, writer);
            	}

			} catch (IOException e) {
//					e1.printStackTrace();
			}

			try {
				_socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
//			System.out.println(" --socket closed: " + this.get_fsd_key());
		}
		
	}
	
	static class TimeoutCleaner  implements Runnable {
		public void run() {
			while (true) {
				int gap = 5000;
				try {
					Thread.sleep(gap);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				synchronized(hash_sym) {
					ArrayList fsds = new ArrayList();
					
		        	Iterator<Entry<String, FileServerDescription>> it = _fsd_table.entrySet().iterator();
		        	while (it.hasNext()) {
		        		Entry<String, FileServerDescription> entry = it.next();
		        		FileServerDescription fsd = entry.getValue();
		        		double tick = System.currentTimeMillis();
		        		if (tick - fsd._last_connect_tick > gap) {
			        		// 记下的fsd key，稍后重新更新file_map
			        		fsds.add(fsd);
		        		}
		        	}
		        	
		        	for (int i=0; i<fsds.size(); i++) {
	        			// this fileserver is timeout, remove the fileserver registration	  
		        		FileServerDescription fsd = (FileServerDescription)fsds.get(i);
	        			String fsd_key = fsd._host_ip + "_" + fsd._listening_port;
	        			remove_fileserver(fsd_key);
	        			System.out.println("fileserver logged out: " + fsd_key);
		        	}
				}
	        }
		}
	}

   public static String getMacAddr() {  
       String MacAddr = "";
       String str = "";
       try {
           NetworkInterface NIC = NetworkInterface.getByName("eth0");
           byte[] buf = NIC.getHardwareAddress();
           for (int i = 0; i < buf.length; i++) {
               str = str + byteHEX(buf[i]);
           }
           MacAddr = str.toUpperCase();
       } catch (SocketException e) {
           e.printStackTrace();
           System.exit(-1);
       }
       return MacAddr;
   }

   public static String getLocalIP() {
       String ip = "";
       try {
           Enumeration<?> e1 = (Enumeration<?>) NetworkInterface.getNetworkInterfaces();
           while (e1.hasMoreElements()) {
               NetworkInterface ni = (NetworkInterface) e1.nextElement();
//               if (!ni.getName().equals("en0") && !ni.getName().equals("eth0")) {
               if (false) {
//            	   System.out.println("ni.getName()" + ni.getName());
//                   continue;
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
//                   break;
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
 
	public static void main(String[] args) {
		new Thread(new TimeoutCleaner()).start();
		int listening_port = 8911;
		if (args.length >= 1) {
			listening_port = Integer.parseInt(args[0]);
		}
		
		System.out.println("host IP must be one of the follow(s): ");	getLocalIP();
		System.out.println("listening on port: " + String.valueOf(listening_port));
		ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket(listening_port);
			
			while (DirServer._running) {
				Socket socket = serverSocket.accept();
				new Thread(new DirServer(socket)).start();
			}
		} catch (IOException e) {
			e.printStackTrace(); 
		}
	}

}
