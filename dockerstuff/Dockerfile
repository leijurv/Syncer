FROM ubuntu
run apt-get update
run apt-get install -y tightvncserver firefox
ENV DEBIAN_FRONTEND noninteractive
run apt-get install -y lxterminal lxde-core

run apt-get install -y pulseaudio paprefs

run apt-get install -y software-properties-common


run  apt-add-repository -y "deb http://repository.spotify.com stable non-free"
run apt-key adv --keyserver keyserver.ubuntu.com --recv-keys D2C19886 
run apt-get update -qq 
run apt-get install -y spotify-client

run apt-get install -y nano wget sox curl
run apt-get install -y qdbus

run add-apt-repository ppa:ubuntu-lxc/lxd-stable
run apt-get update
run apt-get install -y golang

run apt-get install -y ffmpeg

run echo "while [ true ]; do pulseaudio; sleep 1; done" > /pulse.bash

run echo "spotify > /dev/null 2> /dev/null & \n cd /stuff \n go build /stuff/multiplex.go \n echo "starting to record" \n ./multiplex < <(parec -d auto_null.monitor)" > /audio.bash

run echo "cd /stuff \n go build /stuff/player.go \n echo "starting player" \n ./player" > /inputplayer.bash

run echo "tightvncserver \n bash /pulse.bash & \n bash /inputplayer.bash & \n sleep 5 \n bash /audio.bash & \n sleep 5 \n firefox http://www.szynalski.com/tone-generator/ & \n tail -F /root/.vnc/*.log" > /main.bash

run echo "dbus-send --print-reply --dest=org.mpris.MediaPlayer2.spotify /org/mpris/MediaPlayer2 org.mpris.MediaPlayer2.Player.PlayPause" > /playpause.bash

run echo "dbus-send --print-reply --dest=org.mpris.MediaPlayer2.spotify /org/mpris/MediaPlayer2 org.mpris.MediaPlayer2.Player.Play" > /play.bash

run echo "dbus-send --print-reply --dest=org.mpris.MediaPlayer2.spotify /org/mpris/MediaPlayer2 org.mpris.MediaPlayer2.Player.Pause" > /pause.bash

run echo "echo \"Playing song \$1\"\ndbus-send --print-reply --dest=org.mpris.MediaPlayer2.spotify /org/mpris/MediaPlayer2 org.mpris.MediaPlayer2.Player.OpenUri \"string:\$1\"" > /selectsong.bash

run echo "bash /selectsong.bash \$(curl https://api.spotify.com/v1/search?q\=\$(echo \$* | sed \"s/ /%20/g\")\&type\=track | grep uri | grep track | head -n 1 | cut -f 4-4 -d \"\\\"\")" > /search.bash

run echo "qdbus org.mpris.MediaPlayer2.spotify /org/mpris/MediaPlayer2 org.mpris.MediaPlayer2.Player.Metadata" > /info.bash

add . /stuff
ENV DISPLAY :1
EXPOSE 5901
ENV USER root
run mkdir /root/.vnc
RUN echo "nachikek\nnachikek\nyspotify\nspotify\n" | vncpasswd
CMD ["bash","/main.bash"]

run sudo curl -L https://yt-dl.org/downloads/latest/youtube-dl -o /usr/local/bin/youtube-dl

run sudo chmod a+rx /usr/local/bin/youtube-dl