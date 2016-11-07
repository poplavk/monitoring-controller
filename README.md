Configuration file is at config/application.conf  

1. Start monitoring-controller  
bin/monitoring-controller.sh 

2. Add storage service address (so we know can call it)  
GET 127.0.0.1:1499/addStorage?host=host&port=port  

3. Add indexing service address (so we can call it)  
GET 127.0.0.1:1499/addStorage?host=host&port=port  

4. Add record to internal table (start monitoring of metric at node)  
POST 127.0.0.1:1499/startMonitoring?host=host&port=port&type=type  

5. Check that record  
GET 127.0.0.1:1499/getMetricsTable  

6. Now you can try to request metrics  
127.0.0.1:1499/getMetrics?host=host&port=port&type=type&timestamp=timestamp  

If there is no record in table for host,port,type then error message will be returned.  
If there is no timestamp parameter, will try to make request to indexing service without timestamp parameter.  
If there is no indexing service/storage service that we know about, error message will be returned.  
I recommend to use Postman for testing purposes  
