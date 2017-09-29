IMAGE=$(docker build . | tail -n 1 | cut -f 3- -d " ")
echo "starting image $IMAGE"
docker run --rm -p 5021:5021 -p 5022:5022 -p 5023:5023 -p 5900:5901 -v /root/spotify/config/:/root/.config/spotify/:ro --name spotify -it $IMAGE
