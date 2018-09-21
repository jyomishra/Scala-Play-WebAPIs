package actor

import akka.actor.{Actor, Props}
import akka.stream.Materializer
import org.joda.time.DateTime
import services.BitCoinService

import scala.concurrent.ExecutionContext.Implicits

case class BitCoinDataPeriod(period:String)
case class BitCoinDataCustomDate(startDate:DateTime, endDate:DateTime)
case class BitCoinAvgCustomDate(startDate:DateTime, endDate:DateTime)
case class BitCoinDatePredictor(days:Int)
case class BuildDataModel()

class BitCoinDataActor()(implicit val materializer: Materializer) extends Actor{

  private val bitCoinDateService = new BitCoinService(self)(Implicits.global, materializer)

  override def receive: Receive = {
    case BitCoinDataPeriod("week") => sender() ! bitCoinDateService.getBitCoinDataByTime(DateTime.now().minusDays(7))
    case BitCoinDataPeriod("month") => sender() ! bitCoinDateService.getBitCoinDataByTime(DateTime.now().minusDays(30))
    case BitCoinDataCustomDate(x, y) => sender() ! bitCoinDateService.getBitCoinDataForCustomData(x, y)
    case BitCoinAvgCustomDate(x, y) => sender() ! bitCoinDateService.getBitCoinRollingAverageForCustomData(x, y)
    case BitCoinDatePredictor(x) => sender() ! bitCoinDateService.getPredictedBitCoinData(x)
    case BuildDataModel() => bitCoinDateService.getBitCoinAveragePrice()
    case x:Any => println("unknown message : " + x.getClass)
  }
}

object BitCoinDataActor {
  def props()(materializer: Materializer) = Props(new BitCoinDataActor()(materializer))
}