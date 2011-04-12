package info.rkuhn.bootstrap

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers

import AST._

/**
 * 
 * This file is (c) 2011, Roland Kuhn
 *
 * License: LGPLv2.1, for more information see COPYING in top-level directory
 */

class ParserSpec extends WordSpec with MustMatchers {

  "A Parser" must {
    "successfully parse correct input" in {
      val simple = "configure MCL with Property 12 end"
      Parser.parse(simple) must be (Right(AST(List(Configuration("MCL", None, List(With("Property", Literal("12"))))))))
    }
    "fail on incorrect input" in {
      val fail = "configure hallo welt buh end"
      Parser.parse(fail).getClass must be (classOf[Left[_, _]])
    }
  }

}