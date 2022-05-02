enablePlugins(UpdatesPlugin)

import sbt._
ThisBuild / versionScheme := Option("early-semver")
ThisBuild / version := "1.2.5"
ThisBuild / description := "A microservice server for Scala"
ThisBuild / organization := "io.github.joacovela16"
ThisBuild / organizationName := "jsoft"
ThisBuild / homepage := Some(url("https://github.com/modux4s/modux"))
ThisBuild / scalacOptions := Seq("-language:implicitConversions")
ThisBuild / licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

resolvers += Resolver.mavenCentral
Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / sonatypeSessionName := s"[sbt-sonatype] ${name.value}-${scalaBinaryVersion.value}-${version.value}"
ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / sonatypeRepository := "https://s01.oss.sonatype.org"
ThisBuild / publishTo := sonatypePublishTo.value

ThisBuild / scmInfo := Some(
	ScmInfo(
		url("https://github.com/modux4s/modux"),
		"scm:git@github.com:modux4s/modux.git"
	)
)

ThisBuild / developers := List(
	Developer(
		id = "joacovela16",
		name = "Joaquin",
		email = "joaquinvelazquezcamacho@gmail.com",
		url = url("https://github.com/joacovela16")
	)
)


lazy val disablingPublishingSettings =
	Seq(
		publish / skip := true,
		publishArtifact := false,
		Test / publishArtifact := false
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
		crossScalaVersions := Deps.scalaVersions,
		name := "modux-common",
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
			Deps.pac4jCore,
			Deps.pac4jOAuth,
			Deps.pac4jJwt,
			Deps.pac4jHttp
		),

	)

lazy val model = (project in file("./modules/model"))
	.aggregate(common)
	.dependsOn(common)
	.settings(
		crossScalaVersions := Deps.scalaVersions,
		name := "modux-model"
	)

lazy val swaggerExportV3 = (project in file("./modules/export/swagger3"))
	.aggregate(devShared, core)
	.dependsOn(devShared, core)
	.settings(
		name := "modux-swagger-v3",
		crossScalaVersions := Deps.scalaVersions,
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
		crossScalaVersions := Deps.scalaVersions,
		akkaDeps,
		libraryDependencies ++= Seq(
			Deps.swaggerCore2,
			Deps.swaggerModel2,
			Deps.swaggerJaxrs2
		)
	)

lazy val devShared = (project in file("./modules/shared"))
	.enablePlugins(BuildInfoPlugin)
	.settings(
		name := "modux-shared",
		crossScalaVersions := Deps.scalaVersions,
		buildInfoKeys := Deps.buildInfo
	)

lazy val plug = (project in file("./modules/plug"))
	.settings(
		//    crossScalaVersions := Nil,
		sbtPlugin := true,
		name := "modux-plug"
	)

lazy val macros = (project in file("./modules/macros"))
	.aggregate(model)
	.dependsOn(model)
	.settings(
		name := "modux-macros",
		crossScalaVersions := Deps.scalaVersions,
		scalacOptions ++= Seq("-language:experimental.macros"),
		libraryDependencies ++= Seq(
			Deps.twirlApi,
			Deps.jacksonDataformatXml,
			Deps.aaltoXmlParser,
			Deps.jacksonModuleScala,
			Deps.macwireMacros,
			Deps.macwireProxy,
			Deps.macwireUtils,
		)
	)

lazy val core = (project in file("./modules/core"))
	.aggregate(macros, devShared)
	.dependsOn(macros, devShared)
	.settings(
		name := "modux-core",
		crossScalaVersions := Deps.scalaVersions,
		libraryDependencies ++= Seq(Deps.graphql, Deps.caffeine)
	)

lazy val kafkaCore = (project in file("./modules/kafka"))
	.dependsOn(core)
	.settings(
		name := "modux-kafka-core",
		crossScalaVersions := Deps.scalaVersions,
		libraryDependencies ++= Seq(Deps.akkaKafka)
	)

lazy val server = (project in file("./modules/server"))
	.aggregate(core)
	.dependsOn(core)
	.settings(
		name := "modux-server",
		crossScalaVersions := Deps.scalaVersions,
	)

lazy val plugin = (project in file("./modules/plugin"))

	.aggregate(server, devShared, swaggerExportV3, swaggerExportV2, kafkaCore, plug)
	.dependsOn(devShared, plug)
	.settings(
		sbtPlugin := true,
		name := "modux-plugin",
		libraryDependencies ++= Seq(
			Deps.xbean,
			Deps.compress
		),
		addSbtPlugin(Deps.sbtNativePackager),
		addSbtPlugin(Deps.twirl),
		addSbtPlugin(Deps.sbtWeb),

	)

lazy val root = (project in file("."))
	.aggregate(plugin)
	.settings(
		name := "modux4s",
		disablingPublishingSettings
	)
