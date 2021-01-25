package olx.scrap

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.jdk.CollectionConverters._
import scala.util.Try

object Parser {

  private val log = LoggerFactory.getLogger(this.getClass)

  def parseHrefs(url: String)(responseBody: String): Option[(String, List[String])] =
    Try {
      val doc: Document = Jsoup.parse(responseBody)
      val links: List[String] = doc
        .select(".detailsLink")
        .asScala
        .map(_.attr("href"))
        .map(
          _.split("#")(0)
        ) // NB: get rid of anchor in url to make url filtering possible
        .toSet
        .toList
      log.info(s"Found {} links in [{}].", links.length, url)
      val nextPage: String =
        Try(doc.select(".pageNextPrev").last().attr("href")).getOrElse("")
      if (links.nonEmpty) Some((nextPage, links)) else None
    }.recover {
      case e =>
        log.error("Error parsing links for [{}]: {}!", url, e.getMessage)
        None
    }.get

  def parseAd(responseBody: String, url: String): Map[String, String] = {
    val soup: Document = Jsoup.parse(responseBody)
    def parseHtml(keyName: String, default: String = "")(
      parse: Document => String
    ): (String, String) = {
      keyName -> Try(parse(soup)).recover {
        case e =>
          log.error(
            "Error parsing {} from [{}]: {}!",
            keyName,
            url,
            e.getMessage
          )
          default
      }.get
    }
    val usrid: String = parseUserID(url)
    val data: Map[String, String] = Map(
      parseHtml("head")(
        _.select(".offer-titlebox h1").asScala.head.text().trim
      ),
      "url" -> url,
      "usrid" -> usrid,
      parseHtml("username")(_.select("div.quickcontact__user-name").text()),
      parseHtml("adv_list_url")(_.select("a.user-offers").attr("href")),
      "created_at" -> LocalDateTime
        .now()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    )
    //        log.info(s"OK: reading content from `$uri`.")
    data
  }

  private def parseUserID(url: String): String =
    Try(
      """https?://.*ID(.*?)\.html.*""".r.findFirstMatchIn(url).get.group(1)
    ).recover {
      case error =>
        log.error("Error parsing userId from {}: {}!", url, error.getMessage)
        ""
    }.get

  def getPhoneToken(body: String, url: String): String = {
    Try(
      """var phoneToken\W*?=\W*?'(.*)';""".r.findFirstMatchIn(body).get.group(1)
    ).recover {
      case error =>
        log.error(
          "Error parsing phoneToken from [{}]: {}!",
          url,
          error.getMessage
        )
        ""
    }.get
  }

  def parsePhones(responseBody: String): String =
    Try {
      val json: JsValue = responseBody.parseJson
      val phones: String = json.convertTo[Phones].value
      phones
    }.recover {
      case e =>
        val m: String = e.getMessage
        log.error("Error parsing phones: {}!", m)
        m
    }.get

   def parseAdvLocations(responseBody: String): String =
       Try {
         val doc: Document = Jsoup.parse(responseBody)

         val locations: String = doc
            .select("td.bottom-cell")
            .asScala
            .map({ td =>
                val location: String = td
                    .select(".breadcrumb")
                    .get(0)
                    .text()
                    .split(",")
                    .head
                    .trim()

                location
            })
            .toSet
            .toList
            .mkString(",")

         locations
       }.recover {
         case e =>
           val m: String = e.getMessage
           log.error("Error parsing locations: {}!", m)
           m
       }.get

  case class Phones(value: String)

  object Phones {
    implicit val valueReader: RootJsonFormat[Phones] = jsonFormat1(Phones.apply)
  }

}
