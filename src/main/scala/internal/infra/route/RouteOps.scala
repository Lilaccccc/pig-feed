package internal.infra.route

import internal.auth.entity.jwt.JwtManager
import internal.auth.enums.account.Role
import internal.auth.enums.jwt.TokenType
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{PublicEndpoint, auth}
import utils.base.ColoredLogger
import utils.result.{ErrorResult, R, throws}
import utils.route.HttpService.ec

import scala.concurrent.Future
import scala.util.Try

object RouteOps extends ColoredLogger {
  extension [O](r: () => R[O]) {
    def handler: R[O] = Try(r()).fold(
      {
        case e: ErrorResult => R.errorWithCode(e.code, e.getMessage)
        case e: Exception   => R.error()
      },
      result => result
    )
  }

  type Context = (userId: Long, role: Role)

  // 统一返回结构体包装
  extension [I, O](endpoint: PublicEndpoint[I, Unit, R[O], Any]) {
    def logic(func: I => O): ServerEndpoint[Any, Future] = {
      endpoint.serverLogicSuccess(input => Future((() => R(func(input))).handler))
    }

    def security(func: (Context, I) => O): ServerEndpoint[Any, Future] = {
      endpoint
        .in(auth.bearer[String]())
        .serverLogicSuccess((input, token) =>
          Future(
            (
              () =>
                JwtManager
                  .parseAndValidateToken(token, TokenType.Access)
                  .fold(
                    err => err.asInstanceOf[Exception].throws(using this),
                    claim => R(func((userId = claim.userId, role = claim.role), input))
                  )
            ).handler
          )
        )
    }

    def securityAsync(func: (Context, I) => Future[O]): ServerEndpoint[Any, Future] = {
      endpoint
        .in(auth.bearer[String]())
        .serverLogicSuccess((input, token) =>
          val tokenResult: Either[Exception & ErrorResult, Context] = JwtManager
            .parseAndValidateToken(token, TokenType.Access)
            .fold(err => Left(err), claim => Right((claim.userId, claim.role)))

          val right = (ctx: Context) =>
            func(ctx, input).map(R(_)).recover {
              case e: ErrorResult => R.errorWithCode(e.code, e.getMessage)
              case e: Exception   => R.error()
            }

          tokenResult.fold(err => Future.successful(R.errorWithCode(err.code, err.getMessage)), right)
        )
    }
  }
}
