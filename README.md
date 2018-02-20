# Video Game View

This repository contains both the server and client code for Video Game View (VGV). The primary purpose of VGV is to provide detailed game information on recorded videos (i.e. that have lost HUD information due to recording). The client (a chrome extension) provides an interface for users to request videos to be processed for match information and to interact with the results of that process. The server processes videos requested by the client and sends necessary data back for the client to recreate a HUD on the video.


## But what does it do?
The VGV workflow is as follows:

### Request Submission (Client)
1. User opens the video page (currently only Youtube) of the video to be processed.
2. User opens the VGV extension (currently only Chrome).
3. The extension establishes an HTML websocket connection to the server. Connection is maintained by regular pings and may reconnect without losing running jobs if connection is terminated and then re-established within a time limit.
4. User uses client to submit video on the current tab for processing. If the video is missing some information that can help in processing (currently dota replay file links) then user is prompted to submit this information himself.
5. Client sends video url and any additional information to the server.

### Job Creation (Server)
6. Server (python tornado server) listens for websocket connections. Established websockets send JSON requests through their websocket. When a request is received, it is tested for validity and used to spawn a job that is added to a FIFO queue.
7. When the user's job comes up in the queue, it spawns a python subprocess that is monitored for completion by the server.

### 


## Supported Platforms
### Browsers (client)
* Google Chrome

### Sites (client)
* youtube.com

### Games (client and server)
* dota 2 7.09


## Getting Started

For those who want to use the client, you can download it as a [Google Chrome extension](www.videogameview.com)

MORE TBD


## More on the Client
TBD


## More on the Server
TBD


## Authors

* **Austin Milt** - *Initial work* - [github](https://github.com/austinmilt)

## License

This project is licensed under Apache v2 - see the [LICENSE](LICENSE) file for details