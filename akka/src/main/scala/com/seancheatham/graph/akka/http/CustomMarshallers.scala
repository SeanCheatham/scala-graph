package com.seancheatham.graph.akka.http

import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.{as, entity, provide}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, FromRequestUnmarshaller, Unmarshaller}
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.libs.json.{JsObject, JsValue, Json}

object CustomMarshallers {

  implicit val JsonIteratorMarshaller: ToResponseMarshaller[TraversableOnce[JsValue]] =
    Marshaller.opaque { values =>
      HttpResponse(
        entity = HttpEntity.Chunked.fromData(
          ContentTypes.`application/json`,
          Source.fromIterator(() => values.toIterator map (v => ByteString(v.toString)))
        )
      )
    }

  implicit val JsonMapUnmarshaller: FromEntityUnmarshaller[Map[String, JsValue]] = {
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(`application/json`)
      .mapWithCharset {
        case (ByteString.empty, _) => throw Unmarshaller.NoContentException
        case (data, charset) => data.decodeString(charset.nioCharset.name)
      }
      .map(data => Json.parse(data).as[JsObject].value.toMap)
  }

  /**
    * https://github.com/akka/akka-http/issues/284
    */
  def optionalEntity[T](unmarshaller: FromRequestUnmarshaller[T]): Directive1[Option[T]] =
    entity(as[String]).flatMap { stringEntity =>
      if (stringEntity == null || stringEntity.isEmpty) {
        provide(Option.empty[T])
      } else {
        entity(unmarshaller).flatMap(e => provide(Some(e)))
      }
    }
}
