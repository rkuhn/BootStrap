package info.rkuhn.bootstrap

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers

/**
 * 
 * This file is (c) 2011, Roland Kuhn
 *
 * License: LGPLv2.1, for more information see COPYING in top-level directory
 */

class ContextSpec extends WordSpec with MustMatchers {

  "A Context" must {

    "instantiate a simple object" in {
      val ast = Parser.parse("configure TCSimple simple end").right.get
      val ctx = Context(path = List("info.rkuhn.bootstrap."))
      val after = ctx.load(ast)
      after must be (ctx.copy(dict = Map("simple" -> new TCSimple)))
    }

  }

}

case class TCSimple