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
    Compile / sourceDirectory := baseDirectory.value / "app",
    Compile / scalaSource := baseDirectory.value / "app",
    Compile / javaSource := baseDirectory.value / "app",

    Compile / TwirlKeys.compileTemplates / sourceDirectories := Seq((Compile / sourceDirectory).value),
    Test / TwirlKeys.compileTemplates / sourceDirectories := Seq((Compile / sourceDirectory).value),

    Test / sourceDirectory := baseDirectory.value / "test",
    Test / scalaSource := baseDirectory.value / "test",
    Test / javaSource := baseDirectory.value / "test",

    Compile / resourceDirectory := baseDirectory.value / "conf",
    Test / resourceDirectory := baseDirectory.value / "conf",

    Assets / sourceDirectory := baseDirectory.value / "assets",
    Assets / resourceDirectory := baseDirectory.value / "public",

    Compile / managedClasspath += (Assets / sourceDirectory).value,

    Assets / packagePrefix := "public/",
    (Runtime / managedClasspath) += (Assets / packageBin).value
  )

  val packageSettings: Seq[Setting[_]] = Seq(
    Universal / mappings ++= directory((Compile / resourceDirectory).value),
    scriptClasspath := {
      Seq("*", "../conf")
    },

    Compile / packageBin / mappings ~= { in =>
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
