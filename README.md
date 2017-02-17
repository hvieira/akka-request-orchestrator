# akka-request-orchestrator
Orchestrates requests using akka actors and akka-http

What this is doing is quite useless but serves to demonstrate request orchestration.

The timeouts are quite large due to initialization of http pool&connections to "external services". Subsequent requests take smaller amounts of time

## Transaction Flow
Issues a request to a "random number generator service" and then depending on the result requesting a search provider main page

## Parallel Flow
Issues requests in parallel to several search main providers main page and reduce the results into a single response

## How to run
### the server
`activator clean run`

### the tests
`activator clean test`
