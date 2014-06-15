# Architecture

The diagram below shows the relationship of the primary components in the system.

!{{#link}}[["architecture diagram"](:website :presentation.website/architecture-diagram)]{{/link}}

## Primary components

### web-server

An asynchronous webserver provided by [http-kit](http://http-kit.org/), which has been tested to handle [600k concurrent connections](http://http-kit.org/600k-concurrent-connection-http-kit.html) with Clojure.

### router

Composes multiple [bidi](https://github.com/juxt/bidi) route structures into a single Ring handler.

### website

Dynamic web pages rendered with a Mustache templating engine. Implemented in `presentation.website`.

### sse

A [HTML5 _server-sent event_](http://www.html5rocks.com/en/tutorials/eventsource/basics/) publication component

### authenticator

Authenticate an incoming request. Provided by [Cylon](https://github.com/juxt/cylon).

### api

The Stripe-inspired REST API. Powered by
[Liberator](http://clojure-liberator.github.io/liberator/)

### spi

The implementation called by the API. A protocol is used ensure loose-coupling between the API and implementations.

### login-form

A login facade provided by [Cylon](https://github.com/juxt/cylon) but rendered with a Shackleton-owned custom renderer.

### user-store

An HMAC hashed and salted password store, used in a placeholder for [OpenIAM](http://forgerock.com/products/open-identity-stack/openam/).

### database

A store for customers, cards and other Stripe-API inspired objects.

## Supporting components

### auth-binding

Ensures every request is authenticated

### ring-binder

This component inserts entries into the incoming Ring request from its dependencies.

### clostache

The chosen implementation engine for Mustache logic-less templates

### channel

A [core.async](http://clojure.com/blog/2013/06/28/clojure-core-async-channels.html) channel used by the SPI to push events to the browser (using Server-Sent Events).

### session-store

To support cookie-based authentication.
