package modux.server.lib

object ConfigBuilder {

  def build(appName: String): String = {
    s"""
       |
       |akka {
       |  loggers = ["akka.event.slf4j.Slf4jLogger"]
       |
       |  loglevel = "info"
       |  stdout-loglevel = "off"
       |
       |  actor {
       |    provider = "cluster"
       |    allow-java-serialization = on
       |
       |    serializers {
       |      kryo = "io.altoo.akka.serialization.kryo.KryoSerializer"
       |    }
       |
       |    serialization-bindings {
       |      "java.io.Serializable" = kryo
       |    }
       |  }
       |
       |  http {
       |    server {
       |      websocket {
       |        periodic-keep-alive-max-idle = 1 second
       |      }
       |    }
       |  }
       |
       |  remote.artery {
       |    canonical.port = 2553
       |    canonical.hostname = localhost
       |  }
       |
       |  cluster {
       |    seed-nodes = [
       |      "akka://$appName@localhost:2553"
       |    ]
       |
       |    sharding {
       |      number-of-shards = 100
       |    }
       |
       |    downing-provider-class = "akka.cluster.sbr.SplitBrainResolverProvider"
       |  }
       |}
       |""".stripMargin
  }
}
