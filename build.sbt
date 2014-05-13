name := "we14-lab4-solution"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  javaJdbc,
  javaCore,
  javaJpa,
  cache,
  "org.hibernate" % "hibernate-entitymanager" % "4.2.12.Final",
  "com.google.code.gson" % "gson" % "2.2",
  "org.twitter4j" % "twitter4j-core" % "4.0.1", //Twitter4j API
  "org.apache.jena" % "apache-jena-libs" % "2.10.1" exclude("org.apache.httpcomponents", "httpclient")
)     

play.Project.playJavaSettings

templatesImport += "scala.collection._"