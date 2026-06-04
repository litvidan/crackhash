docker cp docker-compose.yml node-1:/docker-compose.yml
docker cp mongo-init.js node-1:/mongo-init.js

docker cp gateway.tar node-3:/gateway.tar
docker exec -it node-3 sh -c "docker load -i /gateway.tar"

docker cp worker.tar node-4:/worker.tar
docker exec -it node-4 sh -c "docker load -i /worker.tar"

docker cp worker.tar node-5:/worker.tar
docker exec -it node-5 sh -c "docker load -i /worker.tar"

