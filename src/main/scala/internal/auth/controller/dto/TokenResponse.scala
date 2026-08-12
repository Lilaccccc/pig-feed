package internal.auth.controller.dto

import io.circe.Codec
import sttp.tapir.Schema

final case class TokenResponse(
  accessToken: String,
  tokenType: String,
  expiresInSeconds: Long
) derives Codec, Schema
