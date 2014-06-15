# Presentation

A repository to contain JUXT talks and presentations

## Architecture

The application architecture follows the style and conventions presented by Stuart Sierra in his various talks and makes use of his [component](https://github.com/stuartsierra/component) library.

The first convention is the components making up the system are declared in a namespace called `system`, in this case [presentation.system](src/presentation/system.clj)

## Developer guide

Development workflow is as per [Stuart Sierra's Reloaded Worflow](http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded). Run the repl with `lein repl`

```
lein repl
user>
```

At the prompt type `(dev)`

```
user> (dev)
dev>
```

If everything compiles, the prompt changes to `dev>`.

Now start the system with `(go)`.

```
dev> (go)
```

The system starts up. All the components defined in [presentation.system](src/presentation/system.clj) will start in order. Make changes to any part of the system, typing `(reset)` to stop the components, reload any parts of the system that has changed and restart all the components.

```
dev> (reset)
```

All the tests are run as part of the reset process, check the console for evidence of any test failures or other warnings.

If you have introduced a compilation error in your coding, the system may not reload, and the `(reset)` call will be unavailable. You can restore things by typing `(refresh)`

```
dev> (refresh)
```

If that doesn't work, you last resort is to quit the JVM and start over.
