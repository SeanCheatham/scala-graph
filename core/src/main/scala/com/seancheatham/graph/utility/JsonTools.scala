package com.seancheatham.graph.utility

import play.api.libs.json.{JsNull, JsObject, JsValue}

/**
  * A helper for JSON-related tasks
  */
object JsonTools {

  implicit class JsObjectHelper(obj: JsObject) {

    def withoutNulls: JsObject =
      JsObject(obj.value collect {
        case (key, obj1: JsObject) =>
          key -> obj1.withoutNulls
        case (key, value) if value != JsNull =>
          key -> value
      }
      )

  }

  implicit class JsonMapHelper(map: Map[String, JsValue]) {
    def withoutNulls =
      map collect {
        case (key, obj1: JsObject) =>
          key -> obj1.withoutNulls
        case (key, value) if value != JsNull =>
          key -> value
      }
  }

}
