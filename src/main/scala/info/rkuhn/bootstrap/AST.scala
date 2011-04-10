package info.rkuhn.bootstrap

/**
 * 
 * This file is (c) 2011, Roland Kuhn
 *
 * License: LGPLv2.1, for more information see COPYING in top-level directory
 */

object AST {

  trait Definition

  case class Configuration(ident : String, name : Option[String], args : List[Argument]) extends Definition
  case class Reference(name : String) extends Definition
  case class Literal(data : String) extends Definition

  trait Argument

  case class With(ident : String, definition : Definition) extends Argument
  case class Constructor(definition : Definition) extends Argument
  case class Call(method : String) extends Argument

}