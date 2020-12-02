import sbt._

object Deps {

  //************** VERSIONS **************//
  private lazy val akkaVersion: String = "2.6.8"
  private lazy val akkaHTTPVersion: String = "10.2.0"
  private lazy val scalaLoggingVersion: String = "3.9.2"
  private lazy val xbeanVersion: String = "4.16"
  private lazy val ansiInterpolatorVersion: String = "1.1.0"
  private lazy val self4jVersion: String = "1.7.25"
  private lazy val jacksonVersion: String = "2.11.2"
  private lazy val typeConfigVersion: String = "1.4.0"
  private lazy val logbackClassicVersion: String = "1.2.3"
  private lazy val kryoSerializationVersion: String = "1.1.5"
  private lazy val sbtNativePackagerVersion: String = "1.7.3"
  private lazy val aaltoXmlParserVersion: String = "1.2.2"
  private lazy val configScalaVersion: String = "1.0.3"
  private lazy val scalaTestVersion: String = "3.2.0"
  private lazy val swaggerVersion3: String = "2.1.4"
  private lazy val swaggerVersion2: String = "1.6.2"
  private lazy val javaxrsVersion3: String = "2.1.1"
  private lazy val woodstoxVersion: String = "6.2.1"
  private val compressVersion = "1.20"

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
  lazy val akkaCors: ModuleID = "ch.megard" %% "akka-http-cors" % "1.0.0"
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
  lazy val jacksonXml = "com.fasterxml.jackson.dataformat" % "jackson-dataformat-xml" % jacksonVersion
  lazy val aaltoXmlParser = "com.fasterxml" % "aalto-xml" % aaltoXmlParserVersion
  lazy val woodstoxCore = "com.fasterxml.woodstox" % "woodstox-core" % woodstoxVersion
  // https://mvnrepository.com/artifact/org.apache.commons/commons-compress
  lazy val compress = "org.apache.commons" % "commons-compress" % compressVersion

  //************** DOCUMENTATION **************//
  lazy val swaggerCore3 = "io.swagger.core.v3" % "swagger-core" % swaggerVersion3
  lazy val swaggerModel3 = "io.swagger.core.v3" % "swagger-models" % swaggerVersion3
  lazy val swaggerJaxrs3 = "io.swagger.core.v3" % "swagger-jaxrs2" % swaggerVersion3

  lazy val swaggerCore2 = "io.swagger" % "swagger-core" % swaggerVersion2
  lazy val swaggerModel2 = "io.swagger" % "swagger-models" % swaggerVersion2
  lazy val swaggerJaxrs2 = "io.swagger" % "swagger-jaxrs" % swaggerVersion2
  //************** UTILS **************//
  lazy val ansiInterpolator = "org.backuity" %% "ansi-interpolator" % ansiInterpolatorVersion
  lazy val typeSafeConf = "com.typesafe" % "config" % typeConfigVersion
  lazy val configScala = "com.github.andyglow" %% "typesafe-config-scala" % configScalaVersion
  lazy val javaxrs = "javax.ws.rs" % "javax.ws.rs-api" % javaxrsVersion3
  //************** TESTING **************//
  lazy val scalactic = "org.scalactic" %% "scalactic" % scalaTestVersion
  lazy val scalatest = "org.scalatest" %% "scalatest" % scalaTestVersion % "test"

}
