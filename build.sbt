
import sbt._


ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / description := "A microservice server for Scala"
ThisBuild / organization := "jsoft.modux"
ThisBuild / scalaVersion := "2.12.10"
ThisBuild / scalacOptions := Seq("-language:implicitConversions")

Global / onChangedBuildSource := ReloadOnSourceChanges
ThisBuild / licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

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


lazy val common = (project in file("./modules/common"))
  .settings(
    name := "modux-common",

    enablingPublishingSettings,
    libraryDependencies ++= Seq(
      Deps.configScala,
      Deps.typeSafeConf,
      Deps.xbean,
      Deps.akkaHTTP,
      Deps.kryoSerialization,
      Deps.akkaTypedActor,
      Deps.akkaStream,
      Deps.akkaCors,
      Deps.akkaSharding,
      Deps.scalaLogging,
      Deps.logbackClassic,
      Deps.jacksonDatatypeJsr,
      Deps.jacksonDatatype,
      Deps.jacksonYaml,
      Deps.jacksonXml,
      Deps.swaggerCore,
      Deps.swaggerModel,
      Deps.swaggerJaxrs2,
      Deps.javaxrs,
      Deps.woodstoxCore,

    )
  )

lazy val model = (project in file("./modules/model"))
  .aggregate(common)
  .dependsOn(common)
  .settings(
    name := "modux-model",
    enablingPublishingSettings,

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

lazy val root = (project in file("./"))
  .aggregate(core, devShared)
  .dependsOn(core, devShared)
  .settings(
    sbtPlugin := true,
    name := "modux-plugin",
    enablingPublishingSettings,
    libraryDependencies ++= Seq(
      Deps.xbean,
      Defaults.sbtPluginExtra(Deps.sbtNativePackager, "1.0", "2.12")
    )
  )
