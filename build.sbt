name := "Implicit Mocks"

version := "1.0"

organization := "Lucas Cavalcanti"

//parallelExecution in Test := false

scalaVersion := "2.9.1"

resolvers ++= Seq(
  "snapshots" at "http://scala-tools.org/repo-snapshots",
  "releases"  at "http://scala-tools.org/repo-releases",
  "repo1"     at "http://repo1.maven.org/maven2/")

//libraryDependencies <+= scalaVersion((v:String) => "org.scala-lang" % "scala-compiler" % v % "compile")

libraryDependencies += "org.mockito"             % "mockito-core"          % "1.8.5"           % "test"

libraryDependencies <+= scalaVersion((v:String) => "org.scala-lang" % "scalap" % v % "test")

libraryDependencies <+= scalaVersion((v:String) => "org.scalatest" % ("scalatest_" + v) % "1.6.1" % "test")

fork := true

ivyLoggingLevel := UpdateLogging.Full
