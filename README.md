### P2P File Sharing System in Master/Slave mode

![Alt text](architecture.jpg?raw=true "Peer-to-Peer File Sharing System")<br/>

The P2P file sharing solution consists of four components: Master Server, Slave Server, Client and a server status monitor.

Master Server, Slave Server and Client run on any Java1.6 environment. The monitor client runs on Android 4.3 device or later version.

Implementation details:<br/>

#### Master Server (Directory Server)
The master server receives the registering and unregistering requests from file servers as well as the file requiring requests from the file requirer.<br/>
It possesses two hash-tables. One maps files and file servers, the other maps fileservers and their running status. Each file server is uniquely identified by its host IP and port. Each file is uniquely identified by its file name. The directory server maintains these two hash-tables according to file servers’ register/unregister actions and file update information.<br/>
The master server receives slave requirers’ requests, and returns a file server’s host IP and port which holds that file to the file requirer. It also receives monitors’ requests, and returns the slave servers’ status  to the monitors.<br/>
The master server does not hold any socket connection continuously. Each connection from file server, file requester or monitor finishes in a short period and only processes a single-pass request. Each connection is processed in a separated thread.<br/>

#### Slave Server (File Server)
The slave server has two roles:<br/>
1. Register/unregister itself to the master server and update the file change information to the master server;<br/>
2. Respond the file client’s requests. Transfer the binary file data to the client.<br/>

#### Client (File Requester)
A file retrieving process will be finished into two step:<br/>
1. Send the file name to the master server, and receive the slave server’s host IP and port;<br/>
2. Send the file name to the slave server that connection information received from step 1, and receive the binary file data as well as save them to a local file.<br/>

#### Monitor
The monitor presents a GUI interface running on Android platform to display every slave servers’ running status, such as current connections count, transfer successful count, transfer failure count and connection failure count.<br/>
