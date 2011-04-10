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
  lazy val configs = rep1(config)
  lazy val config : Parser[Configuration] = config_start ~ rep(config_line) <~ config_end ^^ Configuration
  lazy val config_start = "configure" ~> ident ~ opt(not(keyword) ~> ident)
  lazy val config_line = with_line | constructor | call
  lazy val config_end = "end"

  lazy val keyword = "end" | "with" | "construct" | "call"

  lazy val with_line = "with" ~> ident ~ definition ^^ With
  lazy val constructor = "construct" ~> definition ^^ Constructor
  lazy val call = "call" ~> ident ^^ Call

  lazy val definition : Parser[Definition] = config | reference | ident ^^ Literal | quoted ^^ Literal
  lazy val reference = "reference" ~> ident ^^ Reference

  lazy val quoted = (
      """"([^"\\]*(\\\\|\\"))*[^"\\]*"""".r ^^ {s => s.substring(1, s.length - 2).replaceAll("""\\(.)""", "$1")} |
      """\S+""".r
    )

  def parse(s : String) = parseAll(configs, s)
}