# Video Game View

This repository contains both the server and client code for Video Game View (VGV). The primary purpose of VGV is to provide detailed game information on recorded videos (i.e. that have lost HUD information due to recording). The client (a chrome extension) provides an interface for users to request videos to be processed for match information and to interact with the results of that process. The server processes videos requested by the client and sends necessary data back for the client to recreate a HUD on the video.


## But what does it do?
The VGV workflow is as follows:

### Request Submission ([Client](client/deployment/chrome))
1. User opens the video page (currently only Youtube) of the video to be processed.
2. User opens the [VGV extension](client/deployment/chrome/popup/scripts/popup.js) (currently only Chrome).
3. The [extension](client/deployment/chrome/background/websocket_client.js) establishes an HTML websocket connection to the server. Connection is maintained by regular pings and may reconnect without losing running jobs if connection is terminated and then re-established within a time limit.
4. User uses client to submit video on the current tab for processing. If the video is missing some information that can help in processing (currently dota replay file links) then user is prompted to submit this information himself.
5. Client sends video url and any additional information to the server.

### Job Creation ([Server](server))
6. [Server](server/deployment/server.py) (python [tornado](http://www.tornadoweb.org/en/stable/) server) listens for websocket connections. Established websockets send JSON requests through their websocket. When a request is received, it is tested for validity and used to spawn a job that is added to a FIFO queue.
7. When the user's job comes up in the queue, it spawns a python [subprocess](server/deployment/jobs/jobs_tornado.py) that is monitored for completion by the server.

### Job Execution ([Server](server))
8. Two types of data are processed to supply HUD information to the client. The first of these are (currently) dota 2 replays. Dota 2 replays contain a complete account of a given match, sufficient to recreate the match within the dota 2 game client. In [VGV](server/replay_processing/src/ReplayParser.java), each replay is scrubbed for hero ability, item, status, etc at regular intervals using [skadistats clarity 2](https://github.com/skadistats/clarity). 
9. The second type of data is the video itself. Videos frames are processed in [VGV](server/video_processing/src/video/VideoParser.java) at regular intervals specified by the user using [opencv](https://opencv.org/). Video processing uses image detection to detect (1) hero names - [detected by glyphs](server/video_processing/src/training/trainers/NameTrainerAvgImg.java), which is referenced against the dota game database (see [parse_resources.py](server/deployment/utilities/parse_resources.py) and [keys.pkl](server/deployment/resources)) and (2) game clock time - [detected by ANN](server/video_processing/src/training/trainers/ClockTrainerDigitANN.java), which is used to pull the correct replay information out of processed replays.

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