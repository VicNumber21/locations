package com.vportnov.locations.svc

import cats.effect._
import cats.syntax.all._
import org.flywaydb.core.Flyway

import com.vportnov.locations.svc.Config


object DbMigrator:
  def migrate[F[_]: Sync](db: Config.Database): F[Int] =
    for
      initCount <- doMigration(db.adminUrl, "classpath:db/creation", db.admin)
      migrationCount <- doMigration(db.userUrl, "classpath:db/migration", db.admin)
    yield initCount + migrationCount

  private def doMigration[F[_]: Sync](url: String, path: String, admin: Config.Credentials): F[Int] =
    Sync[F].delay {
      Flyway.configure()
        .dataSource(url, admin.login, admin.password)
        .locations(path)
        .load()
        .migrate()
        .migrationsExecuted
    }
