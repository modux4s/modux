# Installing Modux

* Add plugin `addSbtPlugin("jsoft.modux" %% "modux-plugin" % "${var.moduxVersion}")` to your **plugin.sbt**.
* Add resolver `resolvers += Resolver.bintrayRepo("jsoft", "maven")` to your **build.sbt**.
* Enable plugin `enablePlugins(ModuxPlugin)` in your **build.sbt**.