package info.rkuhn.bootstrap

import scala.util.parsing.combinator._

import AST._

/**
 *
 * This file is (c) 2011, Roland Kuhn
 *
 * License: LGPLv2.1, for more information see COPYING in top-level directory
 */
object Parser extends JavaTokenParsers with ImplicitConversions {
  lazy val configs = rep1(config) ^^ ( x => AST(x) )
  lazy val config : Parser[Configuration] = positioned(config_start ~ rep(config_line) <~ config_end ^^ Configuration)
  lazy val config_start = "configure" ~> ident ~ opt(not(keyword) ~> ident)
  lazy val config_line = with_line | constructor | call
  lazy val config_end = "end"

  lazy val keyword = "end" | "with" | "construct" | "call"

  lazy val with_line = positioned("with" ~> ident ~ definition ^^ With)
  lazy val constructor = positioned("construct" ~> definition ^^ Constructor)
  lazy val call = positioned("call" ~> ident ^^ Call)

  lazy val definition : Parser[Definition] = config | reference | positioned(ident ^^ Literal) | positioned(quoted ^^ Literal)
  lazy val reference = positioned("reference" ~> ident ^^ Reference)

  lazy val quoted = (
      """"([^"\\]*(\\\\|\\"))*[^"\\]*"""".r ^^ {s => s.substring(1, s.length - 2).replaceAll("""\\(.)""", "$1")} |
      """\S+""".r
    )

  def apply(s : String)         : Either[String, AST] = convert(parseAll(configs, s))
  def apply(s : CharSequence)   : Either[String, AST] = convert(parseAll(configs, s))
  def apply(r : java.io.Reader) : Either[String, AST] = convert(parseAll(configs, r))

  private def convert(x : ParseResult[AST]) : Either[String, AST] = x match {
    case Success(x, _) => Right(x)
    case Failure(m, _) => Left(m)
    case Error(m, _) => Left(m)
  }
}
