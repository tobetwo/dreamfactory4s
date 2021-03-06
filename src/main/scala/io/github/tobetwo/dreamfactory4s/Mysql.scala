package io.github.tobetwo.dreamfactory4s

import java.util

import io.github.tobetwo.implicits._
import io.github.tobetwo.rest4s.{AbstractRest, K}
import org.apache.commons.lang.StringEscapeUtils


private[dreamfactory4s] class Mysql extends AbstractRest[Mysql] {
  implicit def _2s(a: Any) = a.toString

  override def resource(r: String*): Mysql = super.resource("_table" :: r.toList mkString "/")

  def table(table: String) = resource(table)

  def select(select: String*): Mysql = param("fields", select map (_.deCamelCase + "_") mkString ",")

  def where(exp: LogicExpression) = {
    val newFilter = getOrElse("filter", EmptyExpression) & exp
    val map = Map("filter" -> newFilter)
    copy(map.asInstanceOf[Map[Any, Any]]).copyByNewMap(K.param)(map.mapValues(_.flatten))
  }

  def orderBy(order: Array[String]): Mysql = orderBy(order: _*)

  def orderBy(order: String*): Mysql = param("order", order map (_ deCamelCase) mkString ",")

  def offset(offset: Int) = param("offset", offset)

  def limit(limit: Int) = param("limit", limit)

  private def getError(result: ValueMap) = StringEscapeUtils unescapeHtml
    result("error").asInstanceOf[Map[String, String]].getOrElse("message", "")

  def getEntity: EntityResult= getEntity()

  def getEntity(id: String = ""): EntityResult = {
    val e = if (id.nonEmpty) {
      val response = resource(getOrElse(K.resource, ""), id).getJson
      if (response.isNotError) EntityResult() data response.body
      else EntityResult() success false code response.code errorMsg getError(response.body)
    }
    else if (getParameter("filter").nonEmpty) {
      val r = limit(1).list
      if(r.success && r.count > 0) EntityResult() data r.get(0)
      else EntityResult()
    }
    else EntityResult()
    e.asInstanceOf[EntityResult]
  }


  def listAll = limit(-1).list

  def list: ListResult = {
    val response = param("include_count", true).getJson
    val result = if (response.isNotError) {
      val list = response.body("resource").asInstanceOf[List[Map[String, String]]]
        .map(_.map { case (k, v) => (k.toLowerCase.camelCase, v) })

      val meta = response.body("meta").asInstanceOf[Map[String, BigInt]]

      ListResult(meta("count"), meta.getOrElse("next", -1)) data list
    } else ListResult() success false code response.code errorMsg getError(response.body)
    result.asInstanceOf[ListResult]
  }

  def count = {
    val response = param("count_only", true).send()(_.asString)
    if (response.isNotError) response.body.toInt
    else 0
  }

  def exists: Boolean = exists("")

  def exists(id: String): Boolean =
    if (id.nonEmpty) !getEntity(id).isEmpty
    else !limit(1).list.isEmpty


  private def deCamalCasedJson(entities: Map[String, String]*) =
    entities.map(_.map { case (k, v) => k.deCamelCase + "_" -> v }).toJson

  def save(entities: Map[String, String]*) = param("resource", deCamalCasedJson(entities: _*)).putJson

  def remove: Any = remove()

  def remove(entities: Map[String, String]*): Any =
    param("resource", deCamalCasedJson(entities: _*)).deleteJson

}

case class EntityResult() extends Result[Map[String, String]]

case class ListResult(
  count: BigInt = 0,
  nextOffset: BigInt = -1
) extends Result[List[Map[String, String]]] {
  def get(i: Int) = data(i)

  def hasNextOffset = nextOffset != -1

}

trait Result[T <: Iterable[_]] extends util.HashMap[String, Any] {
  def data(data: T) = put("data", data)

  def success(success: Boolean) = put("success", success)

  def code(code: Int) = put("code", code)

  def errorMsg(msg: String) = put("errorMsg", msg)

  def data: T = getOrDefault("data", null).asInstanceOf[T]

  def success: Boolean = getOrDefault("success", true).asInstanceOf[Boolean]

  def code: Int = getOrDefault("code", 0).asInstanceOf[Int]

  def errorMsg: String = getOrDefault("errorMsg", "").asInstanceOf[String]

  override def put(key: String, value: Any) = {
    super.put(key, value); this
  }

  override def isEmpty: Boolean = !success || data.isEmpty

}