
sys.props.get("plugin.version") match {
  case Some(x) =>    addSbtPlugin("jsoft.modux" % "modux-plugin" % x)
  case _ => addSbtPlugin("jsoft.modux" % "modux-plugin" % "0.1.0-SNAPSHOT")
}


//addSbtPlugin("jsoft.mserver" % "plugin" % "0.1.0-SNAPSHOT")