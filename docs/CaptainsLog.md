## 2020/05/25 - Options to deal with not having to filter ALL the events in the EventStream
Thank you so much ForeverMash for the brainstorm on this!
 
## option the 1st: introduce PlayerInitializedEvent 
**Pros:**
Will provide a nice hook to setup players in tests, without screwing with the main production code too much

**Cons:**
Is going to be much more code to write T_T
 
## option the 2nd: introduce SnapshotEvent to get a "marker" from where to start asserting events
**Pros:**
More work than option 3, might also (at a later stage) impact actual production code

**Cons:**
But getting events to assert is quite simple, more explicit in code (QoL for future devs), re-use of setup in multiple tests will be easier
 
## option the 3rd: only fetching the last X amount of events
**Pros:**
Nice because simple implementation + leaves options in the setupEvents thing.

**Cons:**
Is not really future proof.

## Decision
Going with this for now because it's simply the least amount of code, but will probably refactor towards option 1 at a later stage (when introducing players get assigned a bunch of initial workers)


## 2020/05/25 - Introduced this Captain's Log
Lightweight ADR's (Architectural Decision Records), but basically mis-using it for my own learning purposes.
