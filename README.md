# akka-request-orchestrator
Orchestrates requests using akka actors and akka-http

What this is doing is quite useless but serves to demonstrate request orchestration.

## How to run
### the server
`activator clean run`

### the tests
`activator clean test`

## Transaction Flow
Issues a request to a "random number generator service" and then depending on the number requests a search provider (out of possible 3) main page

`curl localhost:<port>/orchestrate/transaction`

## Parallel Flow
Issues requests in parallel for all search provider main page and reduce the results into a single response

`curl localhost:<port>/orchestrate/parallel`
