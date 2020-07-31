
import sbt._

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.12.12"
ThisBuild / organization := "jsoft.modux"
ThisBuild / scalacOptions := Seq("-language:implicitConversions")

lazy val common = (project in file("./modux/common"))
  .settings(
    name := "modux-common",
    libraryDependencies ++= Seq(
      Deps.macwire,
      Deps.macwireUtil,
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
      Deps.akkaHttpCache,
      Deps.swaggerCore,
      Deps.swaggerModel,
      Deps.swaggerJaxrs2,
      Deps.javaxrs,
    )
  )

lazy val model = (project in file("./modux/model"))
  .aggregate(common)
  .dependsOn(common)
  .settings(
    name := "modux-model"
  )


lazy val devShared = (project in file("./modux/shared"))
  .settings(name := "modux-shared")

lazy val macros = (project in file("./modux/macros"))
  .aggregate(model)
  .dependsOn(model)
  .settings(
    name := "modux-macros",
    scalacOptions ++= Seq("-language:experimental.macros" /*, "-Ymacro-debug-lite"*/),
    libraryDependencies ++= Seq(
      Deps.jacksonDataformatXml,
      Deps.aaltoXmlParser,
      Deps.jacksonModuleScala,
    )
  )

lazy val core = (project in file("./modux/core"))
  .aggregate(macros, devShared)
  .dependsOn(macros, devShared)
  .settings(
    name := "modux-core",
    scalacOptions ++= Seq("-language:experimental.macros" /*, "-Ymacro-debug-lite"*/),
    libraryDependencies ++= Seq(
    )
    //    javacOptions ++= Seq("-Dscala.usejavacp=true")
  )

lazy val root = (project in file("./modux/plugin"))
  .enablePlugins(SbtPlugin)
  .aggregate(core, devShared)
  .dependsOn(core, devShared)
  .settings(
    sbtPlugin := true,
    name := "modux-plugin",
    libraryDependencies ++= Seq(
      Deps.scalactic,
      Deps.scalatest,
    ),
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++ Seq("-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false,
    libraryDependencies ++= Seq(Deps.xbean, sbt.Defaults.sbtPluginExtra(Deps.sbtNativePackager, "1.0", "2.12"))
  )
