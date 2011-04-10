package info.rkuhn.bootstrap

/**
 * 
 * This file is (c) 2011, Roland Kuhn
 *
 * License: LGPLv2.1, for more information see COPYING in top-level directory
 */

object AST {

  sealed trait Definition
  sealed trait DefinitionRewritten

  case class Configuration(ident : String, name : Option[String], args : List[Argument]) extends Definition
  case class Reference(name : String) extends Definition
  case class Literal(data : String) extends Definition with DefinitionRewritten
  case class Object(obj : AnyRef) extends DefinitionRewritten

  sealed trait Argument
  sealed trait ArgumentRewritten

  case class With(ident : String, definition : Definition) extends Argument
  case class Constructor(definition : Definition) extends Argument
  case class Call(method : String) extends Argument with ArgumentRewritten
  case class WithRewritten(ident : String, definition : DefinitionRewritten) extends ArgumentRewritten
  case class ConstructorRewritten(definition : DefinitionRewritten) extends ArgumentRewritten
  case class ArgFail(err : String) extends ArgumentRewritten

}

case class AST(nodes : List[AST.Configuration])