#############
How to Use It
#############

Using BootStrap within an application is done in two basic steps: parsing the
configuration file and processing its contents.

.. _parsing:

Using the Parser
================

The parser converts the input file into an :abbr:`AST (abstract syntax tree)`,
to be consumed by the processing stage. The input file can be provided either
as a :class:`String`, :class:`CharSequence` or :class:`java.io.Reader`::

   val result = Parser(input)
   result.left foreach { msg => println("parsing failed:\n" + msg); return }
   val ast = result.right.get

From Java the usage is similar but a bit more verbose:

.. code-block:: java

   final Either<String, AST> result = Parser.apply(input)
   if (result.isLeft()) {
     System.out.println("parsing failed:\n" + msg);
     return;
   }
   final AST ast = result.right().get();

.. _context:

Running the Processor
=====================

All processing state is encapsulated with class :class:`Context`, and this
class also offers the facility to process an :class:`AST`. You start by
creating a new :class:`Context` and seeding it with class search paths, aliases
or even predefined objects::

   val ctx = Context()
     .search("my.project.a")
     .search("my.project.b")
     .add("ServiceA", new ServiceAImpl)
     .alias("TCP", "my.project.tcp.impl.SocketImpl")

The search path should contain all packages from which the configuration file
shall be able to instantiate classes. A trailing dot may be omitted, the given
string is tried with and without dot as prefix to a given name in order to find
a class. Adding objects to the dictionary by hand enables their use within the
configuration file by way of the :token:`reference` keyword. Finally, aliases
may be used to achieve finer control over which classes may be instantiated by
the configuration file; a literal match for the alias name is replaced by the
alias expansion before trying to load the class. It should be noted that no
dots are allowed in the class name provided after the :token:`configure`
keyword.

.. code-block:: scala

   val loaded_ctx = ctx load ast
   val obj =
     if (loaded_ctx.err isEmpty) {
       loaded_ctx("myObj")
     } else {
       loaded_ctx.err foreach println
       return
     }
