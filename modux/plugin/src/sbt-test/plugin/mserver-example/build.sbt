import modux.shared.{ServerDecl, ServerVar}

resolvers += Resolver.mavenLocal

lazy val root = (project in file("."))
  .settings(
    version := "0.1",
    name := "modux-test",
    organization := "jsoft.mserver",
    scalaVersion := "2.12.12",
    description := "Un proyecto de ejemplo usando Modux",
    servers := Seq(
      ServerDecl(
        "{schema}://localhost:{port}",
        "Ambiente local",
        Map(
          "schema" -> ServerVar("http", "http", "https"),
          "port" -> ServerVar("9000", "9000", "9001"),
        )
      )
    ),
    libraryDependencies ++= Seq(
    )
  ).enablePlugins(ModuxPlugin)

