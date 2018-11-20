package com.example.ziken.dirmonitor;

import com.example.ziken.dirmonitor.ClientSocketEx;

import java.io.*;
import java.util.Arrays;

public class DirClient extends ClientSocketEx {

public static DirClient _dirclient = new DirClient();

static public class Starter implements Runnable {
    private String _host = null;
    private int _port = -1;
    private int _timeout = -1;
    static private boolean _ok  = false;

    public Starter(String srv_host, int srv_port, int timeout) {
        _ok = false;
        _host = srv_host;
        _port = srv_port;
        _timeout = timeout;
    }

    static public boolean is_ok() {
        return _ok;
    }

    @Override
    public void run() {
        _ok = false;
        if (_dirclient.connect(_host, _port, _timeout)) {
            _dirclient.shakehand();
        }
        _ok = true;
    }
}

    static public class FileManifestsRequester  implements Runnable {
        static private String _manifests = "";
        static private boolean _ok = false;

        public FileManifestsRequester() {
            _ok = false;
        }

        static public boolean is_ok() {
            return _ok;
        }

        static public String get_manifests() {
            return _manifests;
        }

        @Override
        public void run() {
            _ok = false;
            _manifests = _dirclient.require_file_manifests();
            _ok = true;
        }
    }

    static public class FileserverStatusRequester  implements Runnable {
        static private String _status = "";
        static private boolean _ok = false;

        public FileserverStatusRequester() {
            _ok = false;
        }

        static public boolean is_ok() {
            return _ok;
        }

        static public String get_status() {
            return _status;
        }

        @Override
        public void run() {
            _ok = false;
            _status = _dirclient.require_fileserver_status();
            _ok = true;
        }
    }

    static public class ReportDirSvr  implements Runnable {
        String _report = "";
        String _fsd = "";
        static private boolean _ok = false;
        public ReportDirSvr(String fsd, String report) {
            _fsd = fsd;
            _report = report;
        }

        @Override
        public void run() {
            _ok = false;
            try {
                _dirclient._socket.send_msg(_report);
                _dirclient._socket.receive_msg();
                _dirclient._socket.send_msg(_fsd);
                _dirclient._socket.receive_msg();
            } catch (Exception e) {
                e.printStackTrace();
            }
            _ok = true;
        }
        static public boolean is_ok() {
            return _ok;
        }
    }

public boolean isconnected() {
    return _socket.isconnected();
}
private boolean shakehand() {
	if (_socket == null) {
		System.out.println("no socket");
		return false;
	}

	try {
		_socket.send_msg("i am a monitor", false);
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

private void leave() {
    try {
        if (!_socket.isconnected()) {
            return;
        }
        _socket.send_msg("disconnect", false);
        this.disconnect();
    } catch (Exception e) {
        e.printStackTrace();
    }
}

private String require_sth(String request) {
    String msg = "";
    try {
        // require filelist
        _socket.send_msg(request, false);
        // receive the string length
        msg = _socket.receive_msg();
        int msg_len = Integer.parseInt(msg);
        // send respond to ask real data
        _socket.send_msg("ok", false);
        // then receive the real data
        msg = _socket.receive_mass(msg_len);
    } catch (Exception e) {
        e.printStackTrace();
    }

    if (msg.equals("\0")) {
        msg = "";
    }

    return msg;
}

private String require_file_manifests() {
    String msg = "";
    try {
        // require filelist
        _socket.send_msg("request file manifests", false);
        // receive the string length
        msg = _socket.receive_msg();
        int msg_len = Integer.parseInt(msg);
        // send respond to ask real data
        _socket.send_msg("ok", false);
        // then receive the real data
        msg = _socket.receive_mass(msg_len);
    } catch (Exception e) {
        e.printStackTrace();
    }

    if (msg.equals("\0")) {
        msg = "";
    }
    return msg;
}

private String require_fileserver_status() {
    String msg = "";
    try {
        // 2nd, require filelist
        _socket.send_msg("request fileserver status", false);
        msg = _socket.receive_msg();
    } catch (Exception e) {
        e.printStackTrace();
    }

    if (msg.equals("\0")) {
        msg = "";
    }
    return msg;
}

}
