# What can be improved in the solution

1. Hikari Connection Pool in svc: https://tpolecat.github.io/doobie/docs/14-Managing-Connections.html#using-a-hikaricp-connection-pool

2. Refined types may be used instead of case class wrappers for id, longitude and latitude (and probably something else).

3. gRpc secure connection needs to be fixed in Docker image. It could be done by moving from alpine to something else or by
   netty-tcnative-boringssl-static probably.

4. Docker reports about some security issues which needs to be fixed.

5. Verify that Dockerfile / compose.yaml best practices applied. 

6. api and svc tuning (processors, context and etc) in compose.yaml

7. Investigate how to use nginx as load balancer with multiple instances of api and svc
   (https://www.nginx.com/blog/nginx-1-13-10-grpc/ , Load Balancing gRPC Calls)
