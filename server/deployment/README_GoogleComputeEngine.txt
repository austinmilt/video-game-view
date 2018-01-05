SERVER SETUP INSTRUCTIONS
-------------------------------------------------------------------------------
A. Create an Ubuntu Compute Engine Instance

B. Install and test a basic Tornado server
    >> sudo apt-get update -y
    >> sudo apt-get upgrade -y
    >> sudo apt-get install python-pip && sudo pip install --update pip
    >> sudo pip install tornado
    >> sudo nano test.py
        
        import tornado.ioloop
        import tornado.web

        class MainHandler(tornado.web.RequestHandler):
            def get(self):
                self.write("Hello, world")

        def make_app():
            return tornado.web.Application([
                (r"/", MainHandler),
            ])

        if __name__ == "__main__":
            app = make_app()
            app.listen(80)
            tornado.ioloop.IOLoop.current().start()
            
    >> python test.py
    
    (in another termninal) >> curl http://localhost:80
    (in a browser) http://<external_ip>:80
    
    
C. Make the server a service that runs on startup (https://askubuntu.com/questions/919054/how-do-i-run-a-single-command-at-startup-using-systemd)
    >> sudo apt-get install upstart
    >> cd ~/deployment
    >> nano ~/deployment/start-server.sh
    
        #!/bin/sh
        exec /usr/bin/python /home/austin_w_milt/deployment/server.py
        
    >> sudo chmod u+x ~/deployment/start-server.sh
    >> sudo cp ~/deployment/vgv.service /etc/systemd/system
    >> sudo systemctl start vgv
    >> sudo systemctl enable vgv
    
        
D. Install Python packages
    1. >> sudo apt-get install python-pip && sudo pip install --upgrade pip
    2. >> sudo pip install youtube-dl
    

E. Install Java Runtime Environment
    1. >> sudo apt-get install default-jre
    
    
F. Copy deployment directory files to your home directory on the server
    
    
G. Build OpenCV (https://stackoverflow.com/questions/42495970/loading-opencv-library-in-linux, https://docs.opencv.org/master/d9/d52/tutorial_java_dev_intro.html)
    1. Install Java dependencies
        >> sudo apt-get install default-jdk
        >> sudo apt-get install ant
        
    2. Install ffmpeg dependencies (https://stackoverflow.com/questions/41200201/opencv-unable-to-stop-the-stream-inappropriate-ioctl-for-device)
        >> sudo apt-get install ffmpeg
        >> sudo apt-get install libavcodec-dev libavformat-dev libavdevice-dev
        >> sudo apt-get install pkg-config
        >> sudo apt-get install cmake
        
    3. Build OpenCV
        >> sudo git clone git://github.com/opencv/opencv.git
        >> cd opencv
        >> sudo git checkout 3.3.1
        >> sudo mkdir build
        >> cd build
        >> sudo cmake -DBUILD_SHARED_LIBS=OFF ..
        
        You should see "java" in "OpenCV Modules: To be built" printout. If not, check the Java dependencies printed below it and install.
        You should also see FFMPEG showing up with YES in the cmake output.
    
        >> sudo make -j1
        >> sudo make install
        
    4. Copy the opencv .so into your java jar dir (must do this even if the .so file is already there)
        >> sudo cp lib/libopencv_java331.so ~/deployment/java/
    
    
J. Change paths in deployment/settings.config and process_url.py
            
            
Configuring client scripts
    o Edit popup.js to point to the correct server address