package info.rkuhn.bootstrap

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers

import java.lang.{Integer => JInt}

/**
 * 
 * This file is (c) 2011, Roland Kuhn
 *
 * License: LGPLv2.1, for more information see COPYING in top-level directory
 */

class ContextSpec extends WordSpec with MustMatchers {

  val ctx = Context(path = List("info.rkuhn.bootstrap."))

  "A Context" must {

    "instantiate a simple object" in {
      val ast = Parser("configure TCSimple simple end").right.get
      val after = ctx load ast
      after must be (ctx.copy(dict = Map("simple" -> new TCSimple)))
    }

    "instantiate a complex object" in {
      val ast = Parser("configure TCwithConstructor complex construct 12 with Value 42 call method end").right.get
      val result = ctx load ast
      result must be (ctx.copy(dict = Map("complex" -> new TCwithConstructor(12))))
      val obj : TCwithConstructor = result("complex").get
      obj.called must be (true)
      obj.value must be (JInt.valueOf(42))
    }

    "instantiate a configurable object" in {
      val ast = Parser("configure TCConfigurable conf with arg hallo with Value 12 end").right.get
      val result = ctx load ast
      result must be (ctx.copy(dict = Map("conf" -> new TCConfigurable)))
      val obj : TCConfigurable = result("conf").get
      obj.d must be (12.0)
      obj.arg must be (List(("arg", "hallo")))
    }

    "instantiate a nested object" in {
      val ast = Parser("configure TCNested nest construct configure TCSimple end end").right.get
      val result = ctx load ast
      result must be (ctx.copy(dict = Map("nest" -> TCNested(TCSimple()))))
    }

    "resolve references" in {
      val ast = Parser("configure TCSimple simple end configure TCNested nest construct reference simple end").right.get
      val result = ctx load ast
      result must be (ctx.copy(dict = Map("simple" -> TCSimple(), "nest" -> TCNested(TCSimple()))))
    }

    "fail when classes mismatch" in {
      val ast = Parser("configure TCConfigurable conf end configure TCNested nest construct reference conf end").right.get
      val result = ctx load ast
      result("conf") must be (Some(new TCConfigurable))
      result("nest") must be (None)
      result.err must not be ('empty)
      result.err.reverse.head.err must include ("mismatch")
    }

  }

}

case class TCSimple()
case class TCwithConstructor(x : Int) {
  var called = false
  def method { called = true }
  var value = JInt.valueOf(0)
  def setValue(z : JInt) { value = z }
}
case class TCConfigurable extends Configurable {
  var arg = List[(String, AnyRef)]()
  override def useBeanSetters = true
  var d = 0.0
  def setValue(v : Double) { d = v }
  def configure(a : List[(String, AnyRef)]) = {
    arg = a
    Success(this)
  }
}
case class TCNested(simple : TCSimple)
