# docker exec -it node-1 sh -c "docker swarm init"
# docker exec -it node-1 sh -c "docker swarm join-token manager"
param(
    $WorkerToken = "SWMTKN-1-6bdtsgotrfovb4haddn3ispx2jmt4ib4f9ahe6e9on106coz5p-98kc4wjjj3t2sbquky3pk96zp 172.17.0.2:2377",
    $ManagerToken = "SWMTKN-1-6bdtsgotrfovb4haddn3ispx2jmt4ib4f9ahe6e9on106coz5p-bx2k1z9ey3eoipe8wutkw7jnt 172.17.0.2:2377"
)
docker exec -it node-2 sh -c "docker swarm join --token $ManagerToken"
docker exec -it node-3 sh -c "docker swarm join --token $ManagerToken"
docker exec -it node-4 sh -c "docker swarm join --token $WorkerToken"
docker exec -it node-5 sh -c "docker swarm join --token $WorkerToken"