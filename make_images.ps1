docker build -t crackhash/gateway:latest -f gateway/Dockerfile .
docker build -t crackhash/worker:latest -f worker/Dockerfile .

docker save -o gateway.tar crackhash/gateway:latest
docker save -o worker.tar crackhash/worker:latest