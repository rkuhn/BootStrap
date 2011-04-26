.. _syntax:

#########################
Configuration File Syntax
#########################

The grammar for BootStrap files is quite simple, if you are that kind of guy
you may directly scroll down to the end of the section.

Lexical Structure and Values
============================

The configuration file syntax is built up of tokens separated by white-space.
The exact nature of the white-space is not relevant, meaning that you may place
line breaks anywhere between tokens and use indentation as you see fit.

There are several types of values, some of which are defined below, but the
most basic are literals. A literal may either be a contiguous non-white-space
string or characters, or it may be a double-quoted string as, where the only
escape sequences are :token:`\\"` for a literal double-quote and :token:`\\\\`
for a literal backslash.

About Names and References
==========================

Every object created may optionally be given a name. This name must be a legal
Java identifier, i.e. it must begin with either an underscore or a latin
character and may be followed by any sequence of alphanumeric characters
(unicode), underscore or dollar sign. In addition, the name must not be equal
to one of the following keywords:

 - :token:`with`
 - :token:`construct`
 - :token:`call`
 - :token:`end`

Upon successful creation of the object, its name is entered into the context
and available for reference by following statements. References may be used at
any location where a value is needed and are initiated by the keyword
:token:`reference` followed by the name of the referenced object.

Object Creation Basics
======================

An object creation is started by the keyword :token:`configure` followed by
the SimpleName of the class and optionally by the object's name to be entered
in the context. After that may follow specific details to be configured on the
object as described in the following sections. The object definition is always
ended by the keyword :token:`end`.

An object definition may be used at the top level or the configuration file or
at any location where a value is needed.

Configuring Plain Java Objects
==============================

When creating plain Java objects, three facilities are offered: constructor
selection, property setters and parameterless method calls. If you are
configuring a normal Java bean, you will probably only need the second kind.

Constructor Selection
+++++++++++++++++++++

The definition of an object may contain zero or more constructor argument
definitions, which are given by the :token:`construct` keyword followed by a
value. The processor will try to find a matching constructor using the exact
sequence of arguments provided.

Property Setters
++++++++++++++++

The object definition may contain zero or more property definitions which start
with the keyword :token:`with` followed by a Java identifier and a value. All
properties are processed in the order given and it is not an error to specify
the same property name multiple times.

Method Calls
++++++++++++

The definition of an object may contain zero or more method call specifications
which are given by the keyword :token:`call` followed by a Java identifier.
The processor will try to call each of the given methods in the sequence they
are given.

Configuring BootStrap Objects
=============================

Classes implementing the :class:`Configurable` trait are treated differently by
the processor. To keep the grammar context-free, the parser will accept the
same object syntax as in the Java object case, but constructor arguments and
method calls will be rejected by the processing stage. Apart from that,
configuring BootStrap object is done exactly like for plan Java objects.

The Grammar
===========

.. productionlist::
   configs     : `configure` *
   configure   : "configure" `ident` `ident` ? `item` * `end`
   end         : "end"
   item        : `with` | `construct` | `call`
   with        : "with" `ident` `definition`
   construct   : "construct" `definition`
   call        : "call" `ident`
   definition  : `config` | `reference` | `ident` | `quoted`
   reference   : "reference" `ident`

:token:`ident` is a legal Java identifier, while :token:`quoted` is a double-quoted
unicode string, where the only escape sequences are :obj:`\\"` for a literal
double-quote and :obj:`\\\\` for a literal backslash.

