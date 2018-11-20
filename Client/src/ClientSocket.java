import java.net.*;
import java.util.Arrays;
import java.io.*;

public class ClientSocket {
private String _ip;
private int _port;
private int _timeout;
public Socket _socket = null;
private DataOutputStream _outputStream = null;
private DataInputStream _inputStream = null;
private Reader _reader = null;
private Writer _writer = null;
char _buff_receive[] = new char[32768];
public String _server_name = "unknown server";

public ClientSocket(String srv_host, int srv_port, int timeout, String srv_name) {
	Arrays.fill(_buff_receive, '0');
    _ip = srv_host;
    _port = srv_port;
    _timeout = timeout;
    _server_name = srv_name;
}

protected void finalize() {
	this.disconnect();
}

public void connect() throws Exception {
	try {
	    _socket = new Socket(_ip, _port);
    } catch (Exception e) {
	    e.printStackTrace();
	    if (_socket != null)
	        _socket.close();
	    
	    throw e;
	}
	
	_inputStream = new DataInputStream(new BufferedInputStream(_socket.getInputStream()));
	_outputStream = new DataOutputStream(_socket.getOutputStream());
    _reader = new InputStreamReader(_socket.getInputStream());
	_writer = new OutputStreamWriter(_socket.getOutputStream());
}

public void send_msg(String msg, boolean showme) throws Exception {
	/*
	try {
		_outputStream.writeUTF(msg);
		_outputStream.flush();
	} catch (Exception e) {
		e.printStackTrace();
		if (_outputStream != null)
			_outputStream.close();
		throw e;
	}
	*/
	
    _writer.write(msg);
    _writer.flush();
    if (showme){
    	System.out.println(_server_name + "_" + _ip + ":" + _port + " <- " + msg);
    }
}

public void send_msg(String msg) throws Exception {
	this.send_msg(msg, true);
}

public String receive_msg() throws Exception {
	return receive_msg(true);
}

public String receive_msg(boolean showme) throws Exception {
	int len = _reader.read(_buff_receive);
	String msg = new String(_buff_receive, 0, len);
	if (showme) {
		System.out.println(_server_name + "_" + _ip + ":" + _port + " -> " + msg);
	}
	return msg;
}

public DataInputStream getInputStream() throws Exception {
	return _inputStream;
}

public boolean isconnected(){
	return _socket.isConnected();
}

public void disconnect() {
	try {
		if (_writer != null)
			_writer.close();
		
		if (_reader != null)
			_reader.close();
		
		if (_outputStream != null)
			_outputStream.close();
		
		if (_inputStream != null)
			_inputStream.close();
		
		if (_socket != null)
			_socket.close();
	} catch (Exception e) {
	}
}

}
