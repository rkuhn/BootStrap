package info.rkuhn.bootstrap

import scala.util.parsing.input.Positional

/**
 * 
 * This file is (c) 2011, Roland Kuhn
 *
 * License: LGPLv2.1, for more information see COPYING in top-level directory
 */

object AST {

  sealed trait Definition extends Positional
  sealed trait DefinitionRewritten extends Positional

  case class Configuration(ident : String, name : Option[String], args : List[Argument]) extends Definition
  case class Reference(name : String) extends Definition
  case class Literal(data : String) extends Definition with DefinitionRewritten
  case class Object(obj : AnyRef) extends DefinitionRewritten

  sealed trait Argument extends Positional
  sealed trait ArgumentRewritten extends Positional

  case class With(ident : String, definition : Definition) extends Argument
  case class Constructor(definition : Definition) extends Argument
  case class Call(method : String) extends Argument with ArgumentRewritten
  case class WithRewritten(ident : String, definition : DefinitionRewritten) extends ArgumentRewritten
  case class ConstructorRewritten(definition : DefinitionRewritten) extends ArgumentRewritten
  case class PositionalString(err : String) extends ArgumentRewritten {
    override def toString = "[%s] %s" format (pos, err)
  }

}

case class AST(nodes : List[AST.Configuration])
