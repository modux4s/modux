import sbt._

object Deps {



  //************** VERSIONS **************//
  private lazy val akkaVersion: String = "2.6.8"
  private lazy val akkaHTTPVersion: String = "10.2.0"
  private lazy val scalaLoggingVersion: String = "3.9.2"
  private lazy val xbeanVersion: String = "4.16"
  private lazy val ansiInterpolatorVersion: String = "1.1.0"
  private lazy val self4jVersion: String = "1.7.25"
  private lazy val jacksonVersion: String = "2.11.1"
  private lazy val typeConfigVersion: String = "1.4.0"
  private lazy val logbackClassicVersion: String = "1.2.3"
  private lazy val kryoSerializationVersion: String = "1.1.5"
  private lazy val sbtNativePackagerVersion: String = "1.7.3"
  private lazy val aaltoXmlParserVersion: String = "1.2.2"
  private lazy val configScalaVersion: String = "1.0.3"
  private lazy val macwireVersion: String = "2.3.6"
  private lazy val scalaTestVersion: String = "3.2.0"
  private lazy val swaggerVersion: String = "2.1.3"
  private lazy val javaxrsVersion: String = "2.0.1"

  //************** DI **************//
  lazy val sbtNativePackager = "com.typesafe.sbt" % "sbt-native-packager" % sbtNativePackagerVersion
  lazy val xbean = "org.apache.xbean" % "xbean-classloader" % xbeanVersion
  //************** AKKA **************//
  lazy val akkaHTTP = "com.typesafe.akka" %% "akka-http" % akkaHTTPVersion
  lazy val akkaLog = "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
  lazy val akkaTypedActor = "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
  lazy val akkaHttpCache = "com.typesafe.akka" %% "akka-http-caching" % akkaHTTPVersion
  lazy val kryoSerialization = "io.altoo" %% "akka-kryo-serialization" % kryoSerializationVersion
  lazy val akkaStream = "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion
  lazy val akkaSharding = "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion
  lazy val akkaCors: ModuleID =  "ch.megard" %% "akka-http-cors" % "1.0.0"
  //************** LOGGING **************//
  lazy val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion
  lazy val self4j = "org.slf4j" % "slf4j-api" % self4jVersion
  lazy val logbackClassic = "ch.qos.logback" % "logback-classic" % logbackClassicVersion
  lazy val logbackCore = "ch.qos.logback" % "logback-core" % logbackClassicVersion
  //************** SERIALIZATION **************//
  lazy val jacksonDataformatXml = "com.fasterxml.jackson.dataformat" % "jackson-dataformat-xml" % jacksonVersion
  lazy val jacksonModuleScala = "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion
  lazy val jacksonDatatype = "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jacksonVersion
  lazy val jacksonDatatypeJsr = "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion
  lazy val jacksonYaml = "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % jacksonVersion
  lazy val aaltoXmlParser = "com.fasterxml" % "aalto-xml" % aaltoXmlParserVersion
  //************** DOCUMENTATION **************//
  lazy val swaggerCore = "io.swagger.core.v3" % "swagger-core" % swaggerVersion
  lazy val swaggerModel = "io.swagger.core.v3" % "swagger-models" % swaggerVersion
  lazy val swaggerJaxrs2 = "io.swagger.core.v3" % "swagger-jaxrs2" % swaggerVersion
  //************** UTILS **************//
  lazy val ansiInterpolator = "org.backuity" %% "ansi-interpolator" % ansiInterpolatorVersion
  lazy val typeSafeConf = "com.typesafe" % "config" % typeConfigVersion
  lazy val configScala = "com.github.andyglow" %% "typesafe-config-scala" % configScalaVersion
  lazy val javaxrs = "javax.ws.rs" % "javax.ws.rs-api" % javaxrsVersion
  //************** TESTING **************//
  lazy val scalactic = "org.scalactic" %% "scalactic" % scalaTestVersion
  lazy val scalatest = "org.scalatest" %% "scalatest" % scalaTestVersion % "test"

}
