
import sbt._

ThisBuild / version := "1.1.0-SNAPSHOT"
ThisBuild / description := "A microservice server for Scala"
ThisBuild / organization := "jsoft.modux"
ThisBuild / scalaVersion := "2.12.12"
ThisBuild / scalacOptions := Seq("-language:implicitConversions")
ThisBuild / resolvers += "io.confluent" at "https://packages.confluent.io/maven/"
ThisBuild / licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

Global / onChangedBuildSource := ReloadOnSourceChanges

bintrayReleaseOnPublish in ThisBuild := false

lazy val disablingPublishingSettings =
  Seq(skip in publish := true, publishArtifact := false)

lazy val enablingPublishingSettings = Seq(
  publishArtifact := true,
  publishMavenStyle := true,
  // http://www.scala-sbt.org/0.12.2/docs/Detailed-Topics/Artifacts.html
  publishArtifact in Test := false,
  // Bintray
  bintrayPackageLabels := Seq("scala", "sbt"),
  bintrayRepository := "maven",
  bintrayVcsUrl := Option("https://github.com/joacovela16/modux"),
  bintrayOrganization := Option("jsoft"),
)

lazy val akkaDeps = Seq(
  libraryDependencies ++= Seq(
    Deps.configScala,
    Deps.typeSafeConf,
    Deps.akkaHTTP,
    Deps.akkaTypedActor,
    Deps.akkaStream,
    Deps.akkaCors,
    Deps.akkaSharding,
  )
)

lazy val common = (project in file("./modules/common"))
  .settings(
    name := "modux-common",
    enablingPublishingSettings,
    akkaDeps,
    libraryDependencies ++= Seq(
      Deps.xbean,
      Deps.kryoSerialization,
      Deps.scalaLogging,
      Deps.logbackClassic,
      Deps.jacksonDatatypeJsr,
      Deps.jacksonDatatype,
      Deps.jacksonYaml,
      Deps.jacksonXml,
      Deps.woodstoxCore,
    ),

  )

lazy val model = (project in file("./modules/model"))
  .aggregate(common)
  .dependsOn(common)
  .settings(
    name := "modux-model",
    enablingPublishingSettings
  )


lazy val swaggerExportV3 = (project in file("./modules/export/swagger3"))
  .aggregate(devShared, core)
  .dependsOn(devShared, core)
  .settings(
    name := "modux-swagger-v3",
    enablingPublishingSettings,
    akkaDeps,
    libraryDependencies ++= Seq(
      Deps.swaggerCore3,
      Deps.swaggerModel3,
      Deps.swaggerJaxrs3,
      Deps.javaxrs,
    )
  )

lazy val swaggerExportV2 = (project in file("./modules/export/swagger2"))
  .aggregate(devShared, core)
  .dependsOn(devShared, core)
  .settings(
    name := "modux-swagger-v2",
    enablingPublishingSettings,
    akkaDeps,
    libraryDependencies ++= Seq(
      Deps.swaggerCore2,
      Deps.swaggerModel2,
      Deps.swaggerJaxrs2
    )
  )

lazy val devShared = (project in file("./modules/shared"))
  .settings(
    name := "modux-shared",
    enablingPublishingSettings,
  )

lazy val macros = (project in file("./modules/macros"))
  .aggregate(model)
  .dependsOn(model)
  .settings(
    name := "modux-macros",
    scalacOptions ++= Seq("-language:experimental.macros"),
    enablingPublishingSettings,
    libraryDependencies ++= Seq(
      Deps.jacksonDataformatXml,
      Deps.aaltoXmlParser,
      Deps.jacksonModuleScala,
    )
  )

lazy val core = (project in file("./modules/core"))
  .aggregate(macros, devShared)
  .dependsOn(macros, devShared)
  .settings(
    name := "modux-core",
    enablingPublishingSettings,
    libraryDependencies ++= Seq()
  )

lazy val kafkaCore = (project in file("./modules/kafka"))
  .dependsOn(core)
  .settings(
    name := "modux-kafka-core",
    enablingPublishingSettings,
    libraryDependencies ++= Seq(Deps.kafka/*, Deps.kafkaSerializer*/)
  )

lazy val server = (project in file("./modules/server"))
  .aggregate(core)
  .dependsOn(core)
  .settings(
    name := "modux-server",
    enablingPublishingSettings,
  )

lazy val root = (project in file("./"))
  .aggregate(server, devShared, swaggerExportV3, swaggerExportV2, kafkaCore)
  .dependsOn(server, devShared)
  .settings(
    sbtPlugin := true,
    name := "modux-plugin",
    enablingPublishingSettings,
    libraryDependencies ++= Seq(
      Deps.xbean,
      Deps.compress,
      Defaults.sbtPluginExtra(Deps.sbtNativePackager, "1.0", "2.12")
    )
  )
