package modux.plugin.core

import com.typesafe.sbt.SbtNativePackager.Universal
import com.typesafe.sbt.packager.Keys.scriptClasspath
import com.typesafe.sbt.packager.MappingsHelper.directory
import com.typesafe.sbt.web.Import.Assets
import com.typesafe.sbt.web.Import.WebKeys.packagePrefix
import play.twirl.sbt.Import.TwirlKeys
import sbt.Keys._
import sbt._

object CommonSettings {

  private final val ExcludeFilter = Seq("properties", "conf", "props", "txt", "xml")

  val moduxVersion: String = buildinfo.BuildInfo.version

  val moduxMacros: ModuleID = "jsoft.modux" %% "modux-macros" % moduxVersion
  val moduxCore: ModuleID = "jsoft.modux" %% "modux-core" % moduxVersion
  val moduxServer: ModuleID = "jsoft.modux" %% "modux-server" % moduxVersion
  val moduxOpenAPIV2: ModuleID = "jsoft.modux" %% "modux-swagger-v2" % moduxVersion
  val moduxOpenAPIV3: ModuleID = "jsoft.modux" %% "modux-swagger-v3" % moduxVersion
  val moduxSerialization: ModuleID = "jsoft.modux" %% "modux-serialization" % moduxVersion
  val moduxKafka: ModuleID = "jsoft.modux" %% "modux-kafka-core" % moduxVersion

  val projectLayout: Seq[Setting[_]] = Seq(
    sourceDirectory in Compile := baseDirectory.value / "app",
    scalaSource in Compile := baseDirectory.value / "app",
    javaSource in Compile := baseDirectory.value / "app",

    sourceDirectories in(Compile, TwirlKeys.compileTemplates) := Seq((sourceDirectory in Compile).value),
    sourceDirectories in(Test, TwirlKeys.compileTemplates) := Seq((sourceDirectory in Compile).value),

    sourceDirectory in Test := baseDirectory.value / "test",
    scalaSource in Test := baseDirectory.value / "test",
    javaSource in Test := baseDirectory.value / "test",

    resourceDirectory in Compile := (baseDirectory in Compile).value / "conf",
    resourceDirectory in Test := (baseDirectory in Compile).value / "conf",
    sourceDirectory in Assets := (baseDirectory in Compile).value / "assets",
    resourceDirectory in Assets := (baseDirectory in Compile).value / "public",

    (managedClasspath in Compile) += (sourceDirectory in Assets).value,

    packagePrefix in Assets := "public/",
    (managedClasspath in Runtime) += (packageBin in Assets).value
  )

  val packageSettings: Seq[Setting[_]] = Seq(
    mappings in Universal ++= directory((resourceDirectory in Compile).value),
    scriptClasspath := Seq("*", "../conf"),

    mappings in(Compile, packageBin) ~= { in =>
      in.filter { case (path, _) =>
        if (path.isDirectory) {
          true
        } else {
          !ExcludeFilter.contains(path.ext)
        }
      }
    }

  )
}
