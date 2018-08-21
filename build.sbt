lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      scalaVersion := "2.12.6",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "little-scala-swagger",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.0.5" % Test,
      "io.circe" %% "circe-core" % "0.9.3",
      "io.circe" %% "circe-generic-extras" % "0.9.3",
      "org.julienrf" %% "enum" % "3.1",
    )
  )