name := """akka-request-orchestrator"""

version := "2.4.11"

scalaVersion := "2.11.8"

val akkaVersion = "2.4.11"
val akkaHttpVersion = "10.0.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" % "akka-http_2.11" % akkaHttpVersion,
  "com.typesafe.akka" % "akka-http-testkit_2.11" % akkaHttpVersion % "test",
  "org.scalatest" % "scalatest_2.11" % "3.0.1" % "test"
)
