# FileBackedCache

This is a simple map implementation that is implemented with a LRU cache of fixed size and serialised files for when then values will not fit into the LRU cache.

A callback is provided to receive notification when an object is deserialised, in case you need to perform any initialisation, for example setting transient fields.

The code is not thread safe.
