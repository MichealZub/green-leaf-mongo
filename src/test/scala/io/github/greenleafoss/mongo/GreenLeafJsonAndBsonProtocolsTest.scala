package io.github.greenleafoss.mongo

import java.time.ZonedDateTime

import ZonedDateTimeOps.Implicits.strToDate

import org.mongodb.scala.bson.ObjectId
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json._

object GreenLeafJsonAndBsonProtocolsTest {

  // MODEL
  case class Test(id: ObjectId, i: Int, l: Long, b: Boolean, zdt: ZonedDateTime)

  // JSON
  trait TestJsonProtocol extends GreenLeafJsonProtocol {
    implicit def testJf: RootJsonFormat[Test] = jsonFormat5(Test.apply)
  }
  object TestJsonProtocol extends TestJsonProtocol

  // BSON
  trait TestBsonProtocol extends TestJsonProtocol with GreenLeafBsonProtocol {
    override implicit def testJf: RootJsonFormat[Test] = jsonFormat(Test.apply, "_id", "i", "l", "b", "zdt")
  }
  object TestBsonProtocol extends TestBsonProtocol
}

class GreenLeafJsonAndBsonProtocolsTest
  extends AnyWordSpec
  with Matchers {

  import GreenLeafJsonAndBsonProtocolsTest._
  private val obj = Test(new ObjectId("5c72b799306e355b83ef3c86"), 1, 0x123456789L, true, "1970-01-01")

  private val json =
    """
      |{
      |  "id": "5c72b799306e355b83ef3c86",
      |  "i": 1,
      |  "l": 4886718345,
      |  "b": true,
      |  "zdt": "1970-01-01 00:00:00"
      |}
    """.stripMargin

  private val bson =
    """
      |{
      |  "_id": {
      |    "$oid": "5c72b799306e355b83ef3c86"
      |  },
      |  "i": 1,
      |  "l": 4886718345,
      |  "b": true,
      |  "zdt": {
      |    "$date": "1970-01-01T00:00:00Z"
      |  }
      |}
    """.stripMargin


  "GreenLeafJsonAndBsonProtocols" should {

    "write Obj as JSON" in {
      import TestJsonProtocol._
      obj.toJson shouldBe json.parseJson
    }

    "read JSON as Obj" in {
      import TestJsonProtocol._
      json.parseJson.convertTo[Test] shouldBe obj
    }

    "write Obj as BSON" in {
      import TestBsonProtocol._
      obj.toJson shouldBe bson.parseJson
    }

    "read BSON as Obj" in {
      import TestBsonProtocol._
      bson.parseJson.convertTo[Test] shouldBe obj
    }

  }

}
