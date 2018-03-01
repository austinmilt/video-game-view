#!/bin/bash

# Remember to make this work after pulling from Windows, you should install dos2unix
#   sudo apt-get install dos2unix -y
#
# And then run with
#   sudo dos2unix initial_setup.sh && chmod u+x initial_setup.sh && ./initial_setup.sh

PROGNAME=$(basename $0)
SERVER_ROOT=$HOME/deployment
SERVER_SERVICE=$SERVER_ROOT/vgv.service
SERVER_STARTSCRIPT=$SERVER_ROOT/start-server.sh
SERVER_SCRIPT=$SERVER_ROOT/server.py
SERVER_JARDIR=$SERVER_ROOT/java
UPSTART_SDIR=/etc/systemd/system

error_exit() {
	echo "ERROR in ${PROGNAME} on line ${1:-"Unknown Error"}" 1>&2
	exit 1
}

echo_and_wait() {
    read -p "line $1: $2" dummy
}

echo_and_wait $LINENO "Copy deployment directory to the server and press any key to continue."

echo; echo; echo "// INITIAL SYSTEM UPGRADE ///////////////////////////////////////////////"
cd $SERVER_ROOT
sudo apt-get update -y || error_exit "$LINENO: Unable to update system."
sudo apt-get upgrade -y || error_exit "$LINENO: Unable to upgrade system."
sudo apt-get install pkg-config || error_exit "$LINENO: Unable to install package manager."
sudo apt-get install cmake || error_exit "$LINENO: Unable to install cmake."
sudo apt-get install dos2unix -y || error_exist "$LINENO: Unable to install dos2unix."

echo; echo; echo "// INSTALLING SERVER PACKAGES ///////////////////////////////////////////"
sudo apt-get install python-pip -y || error_exit "$LINENO: Unable to install pip."
sudo pip install --upgrade pip || error_exit "$LINENO: Unable to update pip."
sudo pip install tornado || error_exit "$LINENO: Unable to install tornado (Python server)."
sudo pip install google-cloud-storage || error_exit "$LINENO: Unable to install google cloud storage."
sudo pip install youtube-dl || error_exit "$LINENO: Unable to install youtube-dl (for downloading youtube videos)."

echo; echo; echo "// CREATING TORNADO SERVER //////////////////////////////////////////////"
sudo apt-get install upstart -y || error_exit "$LINENO: Unable to install upstart (server service manager)."
sudo dos2unix $SERVER_STARTSCRIPT || error_exit "$LINENO: Unable to convert server start script to unix type."
sudo chmod u+x $SERVER_STARTSCRIPT || error_exit "$LINENO: Unable to make server service executable."
sudo cp $SERVER_SERVICE $UPSTART_SDIR || error_exit "$LINENO: Unable to copy server service script to appropriate directory."
sudo systemctl start vgv || error_exit "$LINENO: Unable to start VGV service."
sudo systemctl enable vgv || error_exit "$LINENO: Unable to enable VGV service."

echo; echo; echo "// INSTALLING JAVA COMPONENTS FOR REQUEST PROCESSING ////////////////////"
sudo apt-get install default-jre -y || error_exit "$LINENO: Unable to install Java Runtime Environment."
sudo apt-get install default-jdk -y || error_exit "$LINENO: Unable to install Java Development Kit."
sudo apt-get install ant -y || error_exit "$LINENO: Unable to install ant."
sudo apt-get install ffmpeg -y || error_exit "$LINENO: Unable to install ffmpeg."
sudo apt-get install libavcodec-dev libavformat-dev libavdevice-dev -y || error_exit "$LINENO: Unable to install additional ffmpeg codecs."
sudo git clone git://github.com/opencv/opencv.git || error_exit "$LINENO: Unable to clone opencv git."
cd opencv || error_exit "$LINENO: Unable to change to the opencv git (Did it not download?)."
sudo git checkout 3.3.1 || error_exit "$LINENO: Unable to checkout opencv v3.3.1."
sudo mkdir build || error_exit "$LINENO: Unable to make the opencv build dir."
cd build || error_exit "$LINENO: Unable to change to build dir."
sudo cmake -DBUILD_SHARED_LIBS=OFF .. || error_exit "$LINENO: Unable to execute cmake for opencv."
echo_and_wait $LINENO "Check that java appears in the OpenCV Modules: To be built printout. If not, check the java dependencies, install those, and retry. You should also see FFMPEG showing up with YES in the cmake output."
sudo make -j1 || error_exit "$LINENO: Unable to make opencv."
sudo make install || error_exit "$LINENO: Unable to make install opencv."
cp lib/libopencv_java331.so $SERVER_JARDIR

echo "FINISHED! Remember to copy your SSL-Certificate over to ~/ssl_cert/my-ssl-crt.crt.pem"

