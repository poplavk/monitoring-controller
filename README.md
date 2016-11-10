For Docker, dont forget to log in: docker login
Useful commands:  
To see all containers: docker ps -a  
To see all images: docker images  
To remove container: docker rm \<id/name\>  
To remove image: docker rmi \<id/name\>  

Building docker image from scratch and push it to Docker Hub:

1. Build application  
gradle clean build or ./gradlew clean build  
next, run 'cd build/distributions && tar -xf monitoring-controller.tar && cd ../..'

2. Build Docker image  
docker build -t z1kkurat/monitoring-controller .

3. Verify that it is okay  
docker run -t -i z1kkurat/monitoring-controller /bin/bash

4. Push to Docker Hub  
docker push z1kkurat/monitoring-controller  

Pulling Docker image from Docker Hub and running it:  
We ALWAYS rely on latest tag, so if we do any versioning, that will be for some internal use  

1. Pull  
docker pull z1kkurat/monitoring-controller

2. Run  
docker run -t -p \<port\>:\<port\> -i z1kkurat/monitoring-controller /bin/bash  
where \<port\> is the port u are going to use to access the application (u will need to set chosen port in application configuration file config/application.conf after container starts and before you start the app itself)

Configuration file is at config/application.conf  

Example:  

1. Fix the config file at config/application.conf  
Set the port, timeouts, allowed metric types and so on  

2. Start monitoring-controller  
bin/monitoring-controller.sh  

3. Add storage service address (so we can call it)  
POST 127.0.0.1:1499/addStorage?host=host&port=port  

4. Add indexing service address (so we can call it)  
POST 127.0.0.1:1499/addIndexing?host=host&port=port  

5. Add record to internal table (start monitoring of metric at node)  
POST 127.0.0.1:1499/startMonitoring?host=host&port=port&type=type  

6. Check that record  
GET 127.0.0.1:1499/getMetricsTable  

7. Now you can try to request metrics  
127.0.0.1:1499/getMetrics?host=host&port=port&type=type&timestamp=timestamp  

If there is no record in table for host,port,type then error message will be returned.  
If there is no timestamp parameter, will try to make request to indexing service without timestamp parameter.  
If there is no indexing service/storage service that we know about, error message will be returned.  
I recommend to use Postman for testing purposes  
