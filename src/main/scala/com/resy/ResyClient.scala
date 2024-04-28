package com.resy

import org.apache.logging.log4j.scala.Logging
import play.api.libs.json.{JsArray, Json}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class ResyClient(resyApi: ResyApi) extends Logging {
  private type ReservationMap = Map[String, Map[String, String]]

  /** Tries to find a reservation based on the priority list of requested reservations times.
    * @param date
    *   Date of the reservation in YYYY-MM-DD format
    * @param partySize
    *   Size of the party reservation
    * @param venueId
    *   Unique identifier of the restaurant
    * @param resTimeTypes
    *   Priority list of reservation times and table types.
    * @param millisToRetry
    *   Optional parameter for how long to try to find a reservation in milliseconds
    * @return
    *   configId which is the unique identifier for the reservation
    */
  def findReservations(date: String, partySize: Int, venueId: Int, resTimeTypes: Seq[ReservationTimeType], millisToRetry: Long = 10000): Try[String] = {
    // Implementation refers to retrying logic to attempt booking until successful or time out
  }

  /** Retrieve booking details to proceed with reservation
    * @param configId
    *   Unique identifier for the reservation
    * @param date
    *   Date of reservation
    * @param partySize
    *   Party size
    * @return
    *   Booking details including payment method ID and token
    */
  def getReservationDetails(configId: String, date: String, partySize: Int): Try[BookingDetails] = {
    val detailData = Json.obj("config_id" -> configId, "day" -> date, "party_size" -> partySize).toString()
    val bookingDetailsResp = Try {
      val response = Await.result(resyApi.postReservation(detailData, "application/json"), 5.seconds)
      val resDetails = Json.parse(response)
      val paymentMethodId = (resDetails \ "user" \ "payment_methods" \ 0 \ "id").as[Int]
      val bookToken = (resDetails \ "book_token" \ "value").as[String]
      BookingDetails(paymentMethodId, bookToken)
    }
    bookingDetailsResp.recover {
      case e: Exception =>
        logger.error("Failed to get booking details", e)
        throw e
    }
  }

  /** Final booking action that confirms the reservation
    * @param paymentMethodId
    *   Payment method ID required for securing the booking
    * @param bookToken
    *   Token required for completing the booking
    * @return
    *   Reservation confirmation token
    */
  def bookReservation(paymentMethodId: Int, bookToken: String): Try[String] = {
    val bookingData = Json.obj("paymentMethodId" -> paymentMethodId, "bookToken" -> bookToken).toString()
    val bookResp = Try {
      val response = Await.result(resyApi.postReservation(bookingData, "application/json"), 10.seconds)
      val jsonObj = Json.parse(response)
      jsonObj("resy_token").as[String]
    }
    bookResp.recover {
      case e: Exception =>
        logger.error("Failed to book reservation", e)
        throw e
    }
  }

  // Other methods as required by your processing logic...
}

object ResyClientErrorMessages {
  val bookingFailedMsg = "Could not book the reservation as requested"
  // add other error messages as necessary
}

final case class BookingDetails(paymentMethodId: Int, bookingToken: String)
