package com.myodov.unicherrygarden.connectors

import caliban.client.CalibanClientError.DecodingError
import caliban.client.__Value._

import scala.util.Try

/** Custom ScalarDecoder for Caliban library, capable of parsing GraphQL "Long" values passed like `0x55bbbd36`. */
object ScalarDecoder {
  implicit val long: caliban.client.ScalarDecoder[Long] = {
    case __NumberValue(numValue) =>
      Try(numValue.toLongExact).toEither.left.map(ex => DecodingError(s"Can't build a Long from numeric input $numValue", Some(ex)))
    case __StringValue(strValue) =>
      (strValue.substring(0, 2), strValue.substring(2)) match {
        case ("0x", hexString) =>
          Try(java.lang.Long.parseUnsignedLong(hexString, 16)).toEither.left
            .map(ex => DecodingError(s"Can't build a Long from string input $strValue", Some(ex)))
        case other =>
          Left(DecodingError(s"Can't build a Long from string input $strValue: does not starts from 0x"))
      }
    case other =>
      Left(DecodingError(s"Can't build a Long from input $other"))
  }
}
