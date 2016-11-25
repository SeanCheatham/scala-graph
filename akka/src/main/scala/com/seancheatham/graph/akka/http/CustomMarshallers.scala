package com.seancheatham.graph.akka.http

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.libs.json.{JsObject, JsValue, Json}

object CustomMarshallers {

  implicit val JsonIteratorMarshaller: ToResponseMarshaller[Iterator[JsValue]] =
    Marshaller.opaque { values =>
      HttpResponse(
        entity = HttpEntity.Chunked.fromData(
          ContentTypes.`application/json`,
          Source.fromIterator(() => values map (v => ByteString(v.toString)))
        )
      )
    }

  implicit val JsonIteratorUnmarshaller: FromEntityUnmarshaller[Iterator[JsValue]] =
    Unmarshaller.withMaterializer(_ =>
      implicit mat =>
        _.dataBytes.runFold(Iterator[JsValue]()) {
          case (res, str) =>
            res ++ Iterator.single(Json.parse(str.toByteBuffer.array()))
        }
    )

  implicit val JsonMapUnmarshaller: FromEntityUnmarshaller[Map[String, JsValue]] = {
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(`application/json`)
      .mapWithCharset {
        case (ByteString.empty, _) => throw Unmarshaller.NoContentException
        case (data, charset) => data.decodeString(charset.nioCharset.name)
      }
      .map(data => Json.parse(data).as[JsObject].value.toMap)
  }

  implicit val OptionalJsonMapUnmarshaller: FromEntityUnmarshaller[Option[Map[String, JsValue]]] = {
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(`application/json`)
      .mapWithCharset {
        case (data, charset) => data.decodeString(charset.nioCharset.name)
      }
      .map {
        case data if data.nonEmpty => Some(Json.parse(data).as[JsObject].value.toMap)
        case _ => None
      }
  }

  implicit val JsonMapMarshaller: ToEntityMarshaller[Map[String, JsValue]] =
    Marshaller.withFixedContentType(`application/json`)(value =>
      HttpEntity(JsObject(value).toString)
    )
}
