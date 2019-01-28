package com.github.lashchenko.sjmq

import java.time._
import java.time.format.DateTimeFormatter

trait ZonedDateTimeOps {

  val DatePattern: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)
  val DateTimePattern: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC)
  val DateTimeIsoPattern: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC)

  def parse(str: String, formatter: DateTimeFormatter): ZonedDateTime = ZonedDateTime.parse(str, formatter)
  // https://bugs.openjdk.java.net/browse/JDK-8041360
  def parseDate(str: String): ZonedDateTime = LocalDate.parse(str, DatePattern).atStartOfDay(ZoneOffset.UTC)
  def parseDateTime(str: String): ZonedDateTime = parse(str, DateTimePattern)
  def parseDateTimeIso(str: String): ZonedDateTime = parse(str, DateTimeIsoPattern)

  def print(zdt: ZonedDateTime, formatter: DateTimeFormatter): String = zdt.format(formatter)
  def printDate(zdt: ZonedDateTime): String = print(zdt, DatePattern)
  def printDateTime(zdt: ZonedDateTime): String = print(zdt, DateTimePattern)
  def printDateTimeIso(zdt: ZonedDateTime): String = print(zdt, DateTimeIsoPattern)

  def now: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)

}

object ZonedDateTimeOps extends ZonedDateTimeOps