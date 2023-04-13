package com.vportnov.locations.svc

import org.scalatest.Suites


// Db integration tests are running on the same container to make it faster.
// The problem is that at the moment there is no such solution from testcontainers.
// It mushed to implement custom framework (see DbContainer and AnyDbSpec) with some
// imperative logic.
// To make the logic working, it is important that all test suites are created before
// very first run of test.
// Unfortunately, standard runner creates test classes one by one.
// So to make it working all Db suites should be added to the list below and also
// marked by @DoNotDiscover in source file to prevent double run.

class DbAllTests extends Suites(
  new DbMigratorTest,
  new DbStructureTest,
  new DbStorageSqlTest,
  new DbStorageTest
)
