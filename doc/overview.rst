########
Overview
########

BootStrap is meant to provide application startup configuration, meaning the
creation and wiring of the configurable parts of an application. It is not
meant as a large dependency injection framework, instead it aims at being
light-weight in terms of file syntax, API and library size.

Simple Example
==============

.. code-block:: text

   configure TcpPort main
     with Port 8080
   end

   configure Service blog
     with Prefix "/myblog/"
     with Handler configure BlogHandler end
     with Port main
   end

   configure Service backend
     with Prefix "/backend/"
     with Handler configure BackendHandler end
     with Port main
   end

   configure AdminArea admin
     with Service reference blog
     with Service reference backend
     with Authenticator "ldap://server/"
     with Port main
   end

This simple example configures a hypothetical application server, which shall
listen on port 8080 for incoming connections. There are two services configured
on that port with different handlers which are instantiated inline. In
addition, the AdminArea receives references to these services.

This example is meant to show that configuration file syntax is
straight-forward and can easily be written by humans as well as emitted by
programs, see :ref:`syntax` for more information.

The first token after :token:`configure` is either the SimpleName of a class
which is found in a list of packages supplied to the processor or an alias
provided to the processor. If this class is a plain Java object, it will be
instantiated and configured via reflection, where properties are set using
normal bean setters; see :ref:`beanconfig` for more information. If the class
implements the :class:`Configurable` interface it will be instantiated by
reflection and then configured as described in :ref:`configurable`. The latter
may be used to build objects indirectly via factories.

Implementation Principles
=========================

The parsing and processing of BootStrap files is done using immutable objects.
This enables you to reuse any intermediate state as you wish. Parsing state is
represented as a :obj:`Context` object, which holds the setup information where
and which classes to find as well as names and references to all objects which
have been processed by this context. Hence, you may choose to process a basic
setup file and then use the resulting context to instantiate different
decoupled parts of your application, reusing the references to the basics.
