package com.github.lashchenko.sjmq

import org.scalatest.{Matchers, WordSpec}
import spray.json._

class LongJsonAndBsonFormatTest extends WordSpec with Matchers {

  "LongJsonFormat" should {

    "write small (int) value as JsNumber in JSON" in {
      import ScalaSprayJsonProtocol._
      1L.toJson shouldBe JsNumber(1)
      1024L.toJson shouldBe JsNumber(1024)
    }

    "write large (long) value as JsNumber in JSON" in {
      import ScalaSprayJsonProtocol._
      0x123456789L.toJson shouldBe JsNumber(4886718345L)
    }

    "write small (int) value as number in BSON" in {
      import ScalaSprayBsonProtocol._
      1L.toJson shouldBe JsNumber(1)
      1024L.toJson shouldBe JsNumber(1024)
    }

    "write large (long) value as $numberLong in BSON" in {
      import ScalaSprayBsonProtocol._
      0x123456789L.toJson shouldBe JsObject("$numberLong" -> JsString("4886718345"))
    }

  }

}
