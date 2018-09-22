package services

import actor.{BitCoinAverageCalculator, BuildDataModel}
import akka.actor.ActorRef
import akka.stream.Materializer
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeComparator, Days}
import play.api.libs.json._
import play.api.libs.ws.ahc.AhcWSClient
import smile.classification
import smile.classification.NaiveBayes

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, ExecutionContext}

class BitCoinService(bitCoinActor: ActorRef)(implicit ec: ExecutionContext, materializer: Materializer) {

  private var bitCoinData = getBitCoinData()
  private var dataModel : Option[NaiveBayes] = None
  private var bitCoinAvgIncrease : Option[Double] = None
  private var lastDatePrice : Option[Double] = None

  def getBitCoinData(): (JsLookupResult, JsLookupResult) = {
    val bitCoinDataFuture = AhcWSClient()(materializer).url("https://www.coinbase.com/api/v2/prices/BTC-USD/historic?period=year").get().map { response =>
      response.body[JsValue]
    }
    val bitCoinList = Await.result(bitCoinDataFuture, 5.seconds)
    val bitCoinCurrency = bitCoinList \ "data" \ "currency"
    val bitCoinPrices = bitCoinList \ "data" \ "prices"

    if(bitCoinCurrency.isEmpty || bitCoinPrices.isEmpty) {
      println("Error in reading data. Please have a look.")
    } else {
      bitCoinActor ! BitCoinAverageCalculator()
    }
    bitCoinData = (bitCoinCurrency, bitCoinPrices)

    bitCoinData
  }

  def getBitCoinDataByTime(time: DateTime) = {
    if (bitCoinData._2.isEmpty) {
      List(JsString("Error in reading bitCoin data"))
    } else {
      bitCoinData._2.get match {
        case priceArray: JsArray => {
          priceArray.value.filter(x => DateTimeComparator.getInstance().compare(DateTime.parse((x \ "time").get.as[String], ISODateTimeFormat.dateTimeParser), time) > 0).toList
        }
        case _ => List(JsString("Error in reading bitCoin prices"))
      }
    }
  }

  def getBitCoinDataForCustomData(startDate:DateTime, endDate:DateTime): List[JsValue] = {
    if (bitCoinData._2.isEmpty) {
      List(JsString("Error in reading bitCoin data"))
    } else {
      bitCoinData._2.get match {
        case priceArray: JsArray => {
          priceArray.value.filter(x => DateTimeComparator.getInstance().compare(DateTime.parse((x \ "time").get.as[String], ISODateTimeFormat.dateTimeParser), startDate) > 0 &&
            DateTimeComparator.getInstance().compare(DateTime.parse((x \ "time").get.as[String], ISODateTimeFormat.dateTimeParser), endDate) <= 0).toList
        }
        case _ => List(JsString("Error in reading bitCoin prices"))
      }
    }
  }

  def getBitCoinRollingAverageForCustomData(startDate:DateTime, endDate:DateTime): List[JsValue] = {
    if (bitCoinData._2.isEmpty) {
      List(JsString("Error in reading bitCoin data"))
    } else {
      var jsList:List[JsValue] = List.empty
      bitCoinData._2.get match {
        case priceArray: JsArray => {
          (1 to Days.daysBetween(startDate.toLocalDate(), endDate.toLocalDate()).getDays()).foreach( i => {
            val priceListTillToday = priceArray.value
              .filter(x => DateTimeComparator.getInstance().
                compare(DateTime.parse((x \ "time").get.as[String], ISODateTimeFormat.dateTimeParser), startDate.plusDays(i-1)) >= 0)
              .map(x => (x \ "price").get.as[String].toDouble)
            val avgTillNow = priceListTillToday.sum / priceListTillToday.size
            jsList = jsList.::(JsObject(Seq(
              "time" -> JsString(startDate.plusDays(i-1).toDate.toString),
              "avgPrice" -> JsNumber(avgTillNow.toInt)
            )))
          })
          jsList
        }
        case _ => List(JsString("Error in reading bitCoin prices"))
      }
    }
  }

  def getBitCoinAveragePrice() = {
    var totalPriceSum : Double = 0
    var count = 0
    var firstDayPrice : Double = 0
    bitCoinData._2.get match {
      case priceArray: JsArray => {
        priceArray.value.foreach(x => {
          val price = (x \ "price").as[String].toDouble
          totalPriceSum = totalPriceSum + price
          if(count == 0) lastDatePrice = Some(price)
          if(count == priceArray.value.size - 1) firstDayPrice = price
          count = count + 1
        })
      }
      case _ => {}
    }
    val bitCoinYearAvg = Some(totalPriceSum/count)
    bitCoinAvgIncrease = Some((bitCoinYearAvg.get - firstDayPrice)/count)
    println("First day price for the year : " + firstDayPrice)
    println("Last day price for the year : " + lastDatePrice.get)
    println("Total Avg price for the year : " + (bitCoinYearAvg.get - firstDayPrice))
    println("Total Avg price increase per day : " + bitCoinAvgIncrease)
  }

  def buildBitCoinDataModel = {
    var dates, prices : List[Option[String]] = List.empty
    bitCoinData._2.get match {
      case priceArray:JsArray => {
        priceArray.value.foreach(x => {
          dates = dates.::((x \ "time").toOption.map(_.as[String]))
          prices = prices.::((x \ "price").toOption.map(_.as[String]))
        })
      }
      case _ => List.empty
    }
    val arraydates = dates.map(x => List(DateTime.parse(x.get, ISODateTimeFormat.dateTimeParser).getMillis.toDouble).toArray).toArray
    val nBdataModel = classification.naiveBayes(arraydates, prices.map(x => x.get.toDouble.toInt).toArray, NaiveBayes.Model.GENERAL)
    dataModel = Some(nBdataModel)
  }

  def getPredictedBitCoinData(days:Int) : List[JsValue] = {
    var jsList : List[JsValue] = List.empty
    if(lastDatePrice.isEmpty || bitCoinAvgIncrease.isEmpty)
      List(JsString("Still Calculating results. Try after Some Time."))
    else {
      (1 to days).foreach(i => {
        val time = DateTime.now().plusDays(i)
        jsList = jsList.::(JsObject(Seq(
          "time" -> JsString(time.toString()),
          "price" -> JsNumber((lastDatePrice.get + bitCoinAvgIncrease.get * i).toInt)
        )))
      })
      jsList.reverse
    }
  }
}
