package shop.http.routes.admin

import cats.effect.Sync
import cats.implicits._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server._
import shop.algebras.Categories
import shop.http.auth._
import shop.http.auth.roles.AdminUser
import shop.http.json._

final case class AdminCategoryRoutes[F[_]: Sync](
    categories: Categories[F]
) extends Http4sDsl[F] {

  private[admin] val prefixPath = "/categories"

  private val httpRoutes: AuthedRoutes[AdminUser, F] =
    AuthedRoutes.of {
      case POST -> Root as admin =>
        Ok(s"You have admin rights! $admin")
      //categories.getAll.flatMap(Created(_))
    }

  def routes(authMiddleware: AuthMiddleware[F, AdminUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
