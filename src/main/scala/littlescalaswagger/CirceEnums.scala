package littlescalaswagger

import cats.syntax.either._
import io.circe.syntax._
import enum.Enum
import io.circe.{Decoder, DecodingFailure, Encoder}

object CirceEnums {

  // this is important as default circe sealed trait mapping looks like "ASC":{} instead of just "ASC"
  def enumDecoder[A: Enum]: Decoder[A] =
    Decoder.instance(
      hc =>
        hc.value.asString
          .flatMap(Enum[A].decode(_).toOption)
          .map(Right(_))
          .getOrElse(DecodingFailure(s"Can't parse: ${hc.value.noSpaces}", hc.history).asLeft)
    )

  // this is important as default circe sealed trait mapping looks like "ASC":{} instead of just "ASC"
  def enumEncoder[A: Enum]: Encoder[A] = Encoder.instance(Enum[A].encode(_).asJson)

}
