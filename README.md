Note due to low usage and other responsibilities, this repository (including the Chrome extension, server, and other code) is no longer being actively maintained. If you are interested in contributing or using the extension, open an issue or contact the main author.

# <img align="left" width="100" height="100" src="https://storage.googleapis.com/vgv-marketing/icon.png"> Video Game View


This repository contains both the server and client code for Video Game View (VGV). The primary purpose of VGV is to provide detailed game information on recorded videos (i.e. that have lost HUD information due to recording). The client (a chrome extension) provides an interface for users to request videos to be processed for match information and to interact with the results of that process. The server processes videos requested by the client and sends necessary data back for the client to recreate a HUD on the video. See a screenshot of the client being used below or [a short promo video](https://www.youtube.com/watch?v=vosreYAUUug).

![Example Screenshot 1](https://storage.googleapis.com/vgv-marketing/screenshot2.png "Example Screenshot 1")

## Contents
[Getting Started](#getting-started)  
[Supported Platforms](#supported-platforms)  
[Workflow](#but-what-does-it-do?)  
[Authors](#authors)  
[License](#license)




## Supported Platforms
### Browsers (client)
* Google Chrome

### Sites (client)
* youtube.com

### Games (client and server)
* dota 2 7.09




## Getting Started
### Users
For those who want to use the client, you can download it as a [Google Chrome extension](https://chrome.google.com/webstore/detail/video-game-view/kkpfabidigmnbgabaihhnfiphccgcbgc).

### Content Creators (Youtube)
#### Minimum Requirements
VGV has few requirements to enable the successful processing of your Youtube Dota 2 videos. At minimum, you must record your videos with at least 480p resolution and do not use any HUD skins that will obscure any part of the hero name or the game clock (see below). In addition, the replay slider at the bottom of the screen must be collapsed. If it is showing, the hero name will be pushed up the screen and VGV will not be able to detect the hero name.

![Video recording requirements image](https://storage.googleapis.com/vgv-marketing/content_creators_1.png "Video recording requirements")

#### Additional Information (HIGHLY recommended)
In addition to the minimum requirements when recording your videos, you can greatly enrich the HUD data displayed to users by including replay file information in your video description. There are two ways of specifying replay information in your video description:

1. (HIGHLY preferred over option 2) by giving hyperlinks to replay .dem's, e.g. (or see [this](https://www.youtube.com/watch?v=qG8JpKFPNdE))
   ```
   @videogameview
   0:00 https://mydatabase.com/replays/3674717392.dem
   4:22 https://mydatabase.com/replays/3674237392.bzip2
   @videogameview
   ```
   
2. by giving match IDs
   ```
   @videogameview
   0:00 2474717392
   3:33 3555717392
   10:19 3674717121
   @videogameview
   ```

When you provide this additional information, make sure to follow these rules:
* Replay information in your video description must be surrounded by the @videogameview tags as shown in the above examples.
* Replay dems must either be uncompressed (i.e. a .dem file) or compressed using bzip2 (as when downloaded from the Valve replay database).
* Each replay specification is composed of the start time of the match in the video followed by a space and then the match ID or replay file link.
* Replay start times should be formatted as they appear in the Youtube video player, e.g. 5:55.

#### Why should I supply my own replay files?
Option 2 of supplying replay information relies on the replays to be available on the Valve replay database. In general, replays are only kept for ~10 days. VGV also has a replay database that will (at the time of writing) keep replays that have been previously requested by clients for up to 90 days, though this is subject to change at any time. So, if you want users of VGV to be able to get enriched HUD information far into the future, the ONLY option is to have the replay files hosted somewhere semi-permanently, such as a personal website or one of the website services that analyze replays.

### Contributors
If you wish to contribute code to the project, please contact the [original author](#authors) first or open an "issue" here on github.




## But what does it do?
The VGV workflow is as follows:

### Request Submission ([Client](client/deployment/chrome))
1. User opens the video page (currently only Youtube) of the video to be processed.
2. User opens the [VGV extension](client/deployment/chrome/popup/scripts/popup.js) (currently only Chrome).
3. The [extension](client/deployment/chrome/background/websocket_client.js) establishes an HTML websocket connection to the server. Connection is maintained by regular pings and may reconnect without losing running jobs if connection is terminated and then re-established within a time limit.
4. User uses client to submit video on the current tab for processing. If the video is missing some information that can help in processing (currently dota replay file links) then user is prompted to submit this information himself.
5. Client sends video url and any additional information to the server. While the request is being processed, the client listens for any updates from the server and displays messages to the user in the extension.

### Job Creation ([Server](server))
6. [Server](server/deployment/server.py) (python [tornado](http://www.tornadoweb.org/en/stable/) server) listens for websocket connections. Established websockets send JSON requests through their websocket. When a request is received, it is tested for validity and used to spawn a job that is added to a FIFO queue.
7. When the user's job comes up in the queue, it spawns a python [subprocess](server/deployment/jobs/jobs_tornado.py) that is monitored for completion by the server.

### Job Execution ([Server](server))
8. Two types of data are processed to supply HUD information to the client. The first of these are (currently) dota 2 replays. Dota 2 replays contain a complete account of a given match, sufficient to recreate the match within the dota 2 game client. In [VGV](server/replay_processing/src/ReplayParser.java), each replay is scrubbed for hero ability, item, status, etc at regular intervals using [skadistats clarity 2](https://github.com/skadistats/clarity). 
9. The second type of data is the video itself. Videos frames are processed in [VGV](server/video_processing/src/video/VideoParser.java) at regular intervals specified by the user using [opencv](https://opencv.org/). Video processing uses image detection to detect (1) hero names - [detected by glyphs](server/video_processing/src/training/trainers/NameTrainerAvgImg.java), which is referenced against the dota game database (see [parse_resources.py](server/deployment/utilities/parse_resources.py) and [keys.pkl](server/deployment/resources)) and (2) game clock time - [detected by ANN](server/video_processing/src/training/trainers/ClockTrainerDigitANN.java), which is used to pull the correct replay information out of processed replays.
10. Replay and video processing results are merged to complete a [time-ordered set of video data](server/deployment/jobs/jobs.py) to pass back to the server.

### Results Transmission ([Server](server))
11. Server performs final cleanup and sends results in compressed gzip format to the client to display.

### HUD Viewing ([Client](client/deployment/chrome))
12. Client websocket listens for a results message from the server containing video processing results. Upon reception, these results are [processed](client/deployment/chrome/background/websocket_client.js) and saved to a semi-permanent state in the browser's storage.
13. User is notified that results are ready for viewing.
14. User clicks on viewing results.
15. Current tab is relocated to the video request URL.
16. Content scripts are injected to the Youtube page.
17. [Viewer manager](client/deployment/chrome/page/scripts/master.js) (within the Youtube page) requests results data from the client and uses these to create a time-ordered reference of tooltip information (see various in [page scripts](client/deployment/chrome/page/scripts)) referenced during video playback.
18. [Tooltip manager](client/deployment/chrome/page/scripts/tooltip_manager.js) queries the video time at regular intervals, creating and updating an HTML overlay (the HUD) on the video, parts of which the user can hover to display detailed HUD information about the match.




## Authors

* **Austin Milt** - *Initial work* - [github](https://github.com/austinmilt)


## License

This project is licensed under Apache v2 - see the [LICENSE](LICENSE) file for details