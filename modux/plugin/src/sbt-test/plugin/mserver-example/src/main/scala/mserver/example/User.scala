package mserver.example

import java.sql.{Time, Timestamp, Date => SQLDate}
import java.time._
import java.util.Date

sealed trait Medusa

case class Pepe(
                 name: String,
                 age: Int,
                 t: String,
                 date: Date = new Date(),
                 sqldate: SQLDate = new SQLDate(System.currentTimeMillis()),
                 timestamp: Timestamp = new Timestamp(System.currentTimeMillis()),
                 instant: Instant = ZonedDateTime.now().toInstant,
                 localdatetime: LocalDateTime = LocalDateTime.now(),
                 localdate: LocalDate = LocalDate.now(),
                 time: Time = new Time(System.currentTimeMillis()),
                 localtime: LocalTime = LocalTime.now(),
                 zoneddatetime: ZonedDateTime = ZonedDateTime.now()
               )

case class Car(model: Int)

case class User(name: String)

case class ErrorReport(msg: String)