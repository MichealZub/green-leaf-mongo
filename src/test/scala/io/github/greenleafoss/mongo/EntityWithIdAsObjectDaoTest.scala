package io.github.greenleafoss.mongo

import java.time.ZonedDateTime
import java.util.UUID

import ZonedDateTimeOps._
import GreenLeafMongoDao.DaoBsonProtocol
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.MongoCollection
import spray.json._

import scala.concurrent.Future

object EntityWithIdAsObjectDaoTest {
  object ExchangeRateModel {

    // MODEL

    // https://exchangeratesapi.io/

    object Currency extends Enumeration {
      type Currency = Value

      val USD = Value("USD")
      val GBP = Value("GBP")
      val CAD = Value("CAD")
      val PLN = Value("PLN")
      val JPY = Value("JPY")
      val EUR = Value("EUR")
      // ...
    }

    import Currency._

    // ID as object { "id": { "base": "USD", "date": "2019-01-18" }, "rates": ... }
    case class ExchangeRateId(base: Currency, date: ZonedDateTime)
    // In official driver macro codecs don't allow to use Enum value as key in map and BigDecimals
    case class ExchangeRate(id: ExchangeRateId, rates: Map[Currency, BigDecimal], updated: ZonedDateTime = now)

    // JSON
    trait ExchangeRateJsonProtocol extends GreenLeafJsonProtocol {
      implicit lazy val ExchangeRateCurrencyFormat: JsonFormat[Currency] = enumToJsonFormatAsString(Currency)
      implicit lazy val ExchangeRateIdFormat: RootJsonFormat[ExchangeRateId] = jsonFormat2(ExchangeRateId.apply)
      implicit lazy val ExchangeRateFormat: RootJsonFormat[ExchangeRate] = jsonFormat3(ExchangeRate.apply)
    }

    object ExchangeRateJsonProtocol extends ExchangeRateJsonProtocol

    // BSON

    class ExchangeRateBsonProtocol
      extends ExchangeRateJsonProtocol
      with GreenLeafBsonProtocol
      with DaoBsonProtocol[ExchangeRateId, ExchangeRate] {

      override implicit lazy val ExchangeRateFormat: RootJsonFormat[ExchangeRate] =
        jsonFormat(ExchangeRate.apply, "_id", "rates", "updated")

      override implicit val idFormat: RootJsonFormat[ExchangeRateId] = ExchangeRateIdFormat
      override implicit val entityFormat: RootJsonFormat[ExchangeRate] = ExchangeRateFormat
    }

  }

  import ExchangeRateModel._

  class ExchangeRateDao(collectionName: String) extends TestGreenLeafMongoDao[ExchangeRateId, ExchangeRate] {

    override protected val collection: MongoCollection[Document] = db.getCollection(collectionName)

    override protected val protocol: ExchangeRateBsonProtocol = new ExchangeRateBsonProtocol
    import protocol._

    def findByDate(date: ZonedDateTime): Future[Seq[ExchangeRate]] = {
      find("_id.date" $eq date)
    }

    def findByDateGt(date: ZonedDateTime): Future[Seq[ExchangeRate]] = {
      find("_id.date" $gt date)
    }

    def findByDateGte(date: ZonedDateTime): Future[Seq[ExchangeRate]] = {
      find("_id.date" $gte date)
    }

  }

  object ExchangeRateDao {
    def apply(): ExchangeRateDao = new ExchangeRateDao("test-exchange-rate-" + UUID.randomUUID())
  }
}

class EntityWithIdAsObjectDaoTest extends TestMongoServer {

  import ZonedDateTimeOps.Implicits.strToDate
  import EntityWithIdAsObjectDaoTest._
  import ExchangeRateModel._
  import Currency._

  private val ExchangeRates = Map[String, ExchangeRate](

    // https://api.exchangeratesapi.io/2019-01-02?base=USD&symbols=EUR,USD,GBP,PLN,CAD,JPY
    "2019-01-02" -> ExchangeRate(
      id = ExchangeRateId(USD, "2019-01-02"),
      rates = Map(
        USD -> BigDecimal(1.0),
        EUR -> BigDecimal(0.8774238835),
        PLN -> BigDecimal(3.769763973),
        GBP -> BigDecimal(0.7911292445),
        CAD -> BigDecimal(1.3641309116),
        JPY -> BigDecimal(109.0462402387)
      )
    ),

    // https://api.exchangeratesapi.io/2019-01-03?base=USD&symbols=EUR,USD,GBP,PLN,CAD,JPY
    "2019-01-03" -> ExchangeRate(
      id = ExchangeRateId(USD, "2019-01-03"),
      rates = Map(
        USD -> BigDecimal(1.0),
        EUR -> BigDecimal(0.8812125485),
        PLN -> BigDecimal(3.787010927),
        GBP -> BigDecimal(0.7958406768),
        CAD -> BigDecimal(1.3563623546),
        JPY -> BigDecimal(107.6929855481)
      )
    ),

    // https://api.exchangeratesapi.io/2019-01-04?base=USD&symbols=EUR,USD,GBP,PLN,CAD,JPY
    "2019-01-04" -> ExchangeRate(
      id = ExchangeRateId(USD, "2019-01-04"),
      rates = Map(
        USD -> BigDecimal(1.0),
        EUR -> BigDecimal(0.8769622029),
        PLN -> BigDecimal(3.7671665351),
        GBP -> BigDecimal(0.7891607472),
        CAD -> BigDecimal(1.3442076646),
        JPY -> BigDecimal(108.0417434009)
      )
    ),

    // https://api.exchangeratesapi.io/2019-01-05?base=USD&symbols=EUR,USD,GBP,PLN,CAD,JPY
    "2019-01-05" -> ExchangeRate(
      id = ExchangeRateId(USD, "2019-01-05"),
      rates = Map(
        USD -> BigDecimal(1.0),
//        EUR -> BigDecimal(0.8769622029),
//        PLN -> BigDecimal(3.7671665351),
//        GBP -> BigDecimal(0.7891607472),
//        CAD -> BigDecimal(1.3442076646),
//        JPY -> BigDecimal(108.0417434009)
      )
    )
  )

  "ExchangeRateDao (id as object)" should {

    "insert one record" in {
      val dao = ExchangeRateDao()
      for {
        insertRes <- dao.insert(ExchangeRates("2019-01-02"))
      } yield {
        insertRes.wasAcknowledged shouldBe true
      }
    }

    "insert multiple records" in {
      val dao = ExchangeRateDao()
      for {
        insertRes <- dao.insert(Seq(ExchangeRates("2019-01-03"), ExchangeRates("2019-01-04")))
      } yield {
        insertRes.getInsertedIds should not be empty
      }
    }

    "find all records" in {
      val dao = ExchangeRateDao()
      for {
        insertRes <- dao.insert(Seq(
          ExchangeRates("2019-01-02"), ExchangeRates("2019-01-03"), ExchangeRates("2019-01-04")))
        xAll <- dao.findAll()
      } yield {
        insertRes.getInsertedIds should not be empty
        xAll should contain allElementsOf Set(
          ExchangeRates("2019-01-02"), ExchangeRates("2019-01-03"), ExchangeRates("2019-01-04"))
      }
    }

    "find records by id" in {
      val dao = ExchangeRateDao()
      for {
        insertRes <- dao.insert(Seq(
          ExchangeRates("2019-01-02"), ExchangeRates("2019-01-03"), ExchangeRates("2019-01-04")))
        findRes <- dao.findById(ExchangeRateId(USD, "2019-01-03"))
        getRes <- dao.getById(ExchangeRateId(USD, "2019-01-03"))
      } yield {
        insertRes.getInsertedIds should not be empty
        findRes shouldBe Some(ExchangeRates("2019-01-03"))
        getRes shouldBe ExchangeRates("2019-01-03")
      }
    }

    "find records by id with incorrect fields ordering" in {
      val dao = ExchangeRateDao()

      // 2019-01-03
      // "_id": { "date": { "$date": 1546473600000 }, "base": "USD" },
      val d1 = Document(
        """
          |{
          |  "_id": { "date": { "$date": "2019-01-03T00:00:00.000Z" }, "base": "USD" },
          |  "rates": {
          |    "PLN": 3.787010927,
          |    "CAD": 1.3563623546,
          |    "GBP": 0.7958406768,
          |    "JPY": 107.6929855481,
          |    "USD": 1.0,
          |    "EUR":0.8812125485
          |  },
          |  "updated": { "$date": 1548022714195 }
          |}
        """.stripMargin
      )

      // 2019-01-04
      // "_id": { "date": { "$date": 1546560000000 }, "base": "USD" },
      val d2 = Document(
        """
          |{
          |  "_id": { "date": { "$date": "2019-01-04T00:00:00.000Z" }, "base": "USD" },
          |  "rates": {
          |    "PLN": 3.7671665351,
          |    "CAD": 1.3442076646,
          |    "GBP": 0.7891607472,
          |    "JPY": 108.0417434009,
          |    "USD": 1.0,
          |    "EUR": 0.8769622029
          |  },
          |  "updated": { "$date": 1548022714195 }
          |}
        """.stripMargin
      )

      for {
        insertRes <- dao.insertDocuments(d1, d2)
        findRes <- dao.findById(ExchangeRateId(USD, "2019-01-03"))
        getRes <- dao.getById(ExchangeRateId(USD, "2019-01-03"))
      } yield {
        insertRes.getInsertedIds should not be empty
        val resetUpdated = now
        findRes.map(_.copy(updated = resetUpdated)) shouldBe Some(ExchangeRates("2019-01-03").copy(updated = resetUpdated))
        getRes.copy(updated = resetUpdated) shouldBe ExchangeRates("2019-01-03").copy(updated = resetUpdated)
      }
    }

    "find records by ids" in {
      val dao = ExchangeRateDao()
      for {
        insertRes <- dao.insert(Seq(
          ExchangeRates("2019-01-02"), ExchangeRates("2019-01-03"), ExchangeRates("2019-01-04")))
        x <- dao.findByIdsOr(Seq(ExchangeRateId(USD, "2019-01-03"), ExchangeRateId(USD, "2019-01-04")))
        y <- dao.findByIdsOr(Seq(ExchangeRateId(USD, "2019-01-02")))
      } yield {
        insertRes.getInsertedIds should not be empty
        x should contain allElementsOf Set(ExchangeRates("2019-01-03"), ExchangeRates("2019-01-04"))
        y should contain allElementsOf Set(ExchangeRates("2019-01-02"))
      }
    }

    "find records by filter" in {
      val dao = ExchangeRateDao()
      for {
        insertRes <- dao.insert(Seq(
          ExchangeRates("2019-01-02"), ExchangeRates("2019-01-03"), ExchangeRates("2019-01-04")))
        x <- dao.findByDate(date = "2019-01-02")
        y <- dao.findByDateGt(date = "2019-01-03")
        z <- dao.findByDateGte(date = "2019-01-04")
      } yield {
        insertRes.getInsertedIds should not be empty
        x should contain allElementsOf Set(ExchangeRates("2019-01-02"))
        y should contain allElementsOf Set(ExchangeRates("2019-01-04"))
        z should contain allElementsOf Set(ExchangeRates("2019-01-04"))
      }
    }

    "insert and update records" in {
      val dao = ExchangeRateDao()

      val id = ExchangeRateId(USD, "2019-01-05")
      val oldRate = ExchangeRates("2019-01-05")
      val newRate = ExchangeRate(
        id = ExchangeRateId(USD, "2019-01-05"),
        rates = Map(
          USD -> BigDecimal(1.0),
          EUR -> BigDecimal(0.8769622029),
          PLN -> BigDecimal(3.7671665351),
          GBP -> BigDecimal(0.7891607472),
          CAD -> BigDecimal(1.3442076646),
          JPY -> BigDecimal(108.0417434009)
        )
      )

      for {
        insertRes <- dao.insert(oldRate)
        findRes1 <- dao.findById(id)
        getRes1 <- dao.getById(id)
        updateRes <- dao.replaceById(id, newRate)
        findRes2 <- dao.findById(id)
        getRes2 <- dao.getById(id)
      } yield {
        insertRes.wasAcknowledged shouldBe true
        findRes1 shouldBe Some(oldRate)
        getRes1 shouldBe oldRate
        updateRes shouldBe Some(oldRate)
        findRes2 shouldBe Some(newRate)
        getRes2 shouldBe newRate
      }
    }

    "replaceById if previous entity doesn't exist" in {
      val dao = ExchangeRateDao()
      for {
        updateRes <- dao.replaceById(ExchangeRates("2019-01-02").id, ExchangeRates("2019-01-02"))
        findRes <- dao.findById(ExchangeRates("2019-01-02").id)
      } yield {
        updateRes shouldBe None
        // entity doesn't exist and upsert = false by default
        findRes shouldBe None
      }
    }

    "replaceOrInsertById if previous entity doesn't exist" in {
      val dao = ExchangeRateDao()
      for {
        updateRes <- dao.replaceOrInsertById(ExchangeRates("2019-01-02").id, ExchangeRates("2019-01-02"))
        findRes <- dao.findById(ExchangeRates("2019-01-02").id)
        getRes <- dao.getById(ExchangeRates("2019-01-02").id)
      } yield {
        updateRes shouldBe None
        // entity doesn't exist and will be inserted
        findRes shouldBe Some(ExchangeRates("2019-01-02"))
        getRes shouldBe ExchangeRates("2019-01-02")
      }
    }

    "replaceById if previous entity exists" in {
      val dao = ExchangeRateDao()
      val createEntity = ExchangeRates("2019-01-02")
      val updateEntity = ExchangeRates("2019-01-02").copy(rates = Map.empty)
      for {
        insertRes <- dao.insert(createEntity)
        updateRes <- dao.replaceById(createEntity.id, updateEntity)
        findRes <- dao.findById(updateEntity.id)
        getRes <- dao.getById(updateEntity.id)
      } yield {
        insertRes.wasAcknowledged shouldBe true
        updateRes shouldBe Some(createEntity)
        findRes shouldBe Some(updateEntity)
        getRes shouldBe updateEntity
      }
    }

    "createOrReplaceById if previous entity exists" in {
      val dao = ExchangeRateDao()
      val createEntity = ExchangeRates("2019-01-02")
      val updateEntity = ExchangeRates("2019-01-02").copy(rates = Map.empty)
      for {
        insertRes <- dao.insert(createEntity)
        updateRes <- dao.createOrReplaceById(createEntity.id, updateEntity)
        findRes <- dao.findById(updateEntity.id)
        getRes <- dao.getById(updateEntity.id)
      } yield {
        insertRes.wasAcknowledged shouldBe true
        updateRes shouldBe Some(createEntity)
        findRes shouldBe Some(updateEntity)
        getRes shouldBe updateEntity
      }
    }

    "replaceOrInsertById if previous entity exists" in {
      val dao = ExchangeRateDao()
      val createEntity = ExchangeRates("2019-01-02")
      val updateEntity = ExchangeRates("2019-01-02").copy(rates = Map.empty)
      for {
        insertRes <- dao.insert(createEntity)
        updateRes <- dao.replaceOrInsertById(createEntity.id, updateEntity)
        findRes <- dao.findById(updateEntity.id)
        getRes <- dao.getById(updateEntity.id)
      } yield {
        insertRes.wasAcknowledged shouldBe true
        updateRes shouldBe Some(createEntity)
        findRes shouldBe Some(updateEntity)
        getRes shouldBe updateEntity
      }
    }

  }

}
