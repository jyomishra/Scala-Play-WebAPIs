package controllers

import javax.inject._

import actor._
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.stream.ActorMaterializer
import org.joda.time.{DateTime, DateTimeComparator}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
import scala.util.Try

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents)(implicit assetsFinder: AssetsFinder)
  extends AbstractController(cc) {

  implicit val timeout: akka.util.Timeout = 15.seconds
  implicit val system = ActorSystem("myActorSystem")
  implicit val materializer = ActorMaterializer()

  val bitCoinDataPool = system.actorOf(RoundRobinPool(5).props(BitCoinDataActor.props()(materializer)))

  import java.text.SimpleDateFormat

  val formatter = new SimpleDateFormat("dd-MMM-yyyy")

  def getBitCoinData = Action.async { implicit request:Request[AnyContent] => {
      val requestParam = request.queryString
      if(requestParam.get("period").isDefined && requestParam("period").head.equals("week")) {
        (bitCoinDataPool ? BitCoinDataPeriod("week")).map(generateOutput)
      } else if(requestParam.get("period").isDefined && requestParam("period").head.equals("month")) {
        (bitCoinDataPool ? BitCoinDataPeriod("month")).map(generateOutput)
      } else if(requestParam.get("startDate").isDefined && requestParam.get("endDate").isDefined) {
        val startDate = Try(DateTime.parse(requestParam("startDate").head))
        val endDate = Try(DateTime.parse(requestParam("endDate").head))
        if(startDate.isFailure || endDate.isFailure || (DateTimeComparator.getInstance().compare(startDate.get, endDate.get) > 0)) {
          BadRequest("StartDate or EndDate or Both are not in correct format DD-MM-YYYY")
        }
        (bitCoinDataPool ? BitCoinDataCustomDate(startDate.get, endDate.get)).map(generateOutput)
      }else {
        Future(BadRequest("wrong Input"))
      }
    }
  }

  def getBitCoinPredicationData = Action.async { implicit request: Request[AnyContent] => {
    val requestParam = request.queryString
    if(requestParam.get("period").isDefined && Try(requestParam("period").head.toInt).isSuccess) {
      (bitCoinDataPool ? BitCoinDatePredictor(requestParam("period").head.toInt)).map(generateOutput)
    } else
      Future(BadRequest("Wrong Input"))
    }
  }

  def getBitCoinRollingAverage = Action.async { implicit request: Request[AnyContent] => {
    val requestParam = request.queryString
    if(requestParam.get("startDate").isDefined && requestParam.get("endDate").isDefined) {
      val startDate = Try(DateTime.parse(requestParam("startDate").head))
      val endDate = Try(DateTime.parse(requestParam("endDate").head))
      if(startDate.isFailure || endDate.isFailure || (DateTimeComparator.getInstance().compare(startDate.get, endDate.get) > 0)) {
        BadRequest("StartDate or EndDate or Both are not in correct format DD-MM-YYYY")
      }
      (bitCoinDataPool ? BitCoinAvgCustomDate(startDate.get, endDate.get)).map(generateOutput)
    } else
      Future(BadRequest("Wrong Input"))
  }
  }

  private def generateOutput(x:Any) : Result = x match {
    case list: List[JsValue] => Ok(Json.toJson(list))
    case x: Any => Ok(x.toString)
  }
}
