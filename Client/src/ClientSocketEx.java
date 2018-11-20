import java.io.*;
import java.util.Arrays;

public class ClientSocketEx{
public ClientSocket  _socket = null;
public String _server_host 		= "uninitialized host";
public int _server_port 		= -1;

public boolean connect(String host, Integer port, Integer timeout, String srv_name) {
	if (_socket == null){
		_socket = new ClientSocket(host, port, timeout, srv_name);
	} else {
		return false;
	}
	
	try {
		_socket.connect();		
		_server_host = host;
		_server_port = port;
		System.out.print("connect to server successfully" + "\n");
		return true;
	} catch (Exception e) {
		System.out.print("connect to server failed" + "\n");
	}

	return false;
}

public void disconnect() {
	_socket.disconnect();
	_socket = null;
}

public String get_server_desc() {
	return _server_host + "_" + String.valueOf(_server_port);
}		

}