# Magnum Sal: Event Sourced

[Magnum Sal](https://boardgamegeek.com/boardgame/73316/magnum-sal) is a fun board game about mining salt out of the famous Polish salt mine [Wieliczka](https://www.wieliczka-saltmine.com/)

## The goal of this repo
Play around with EventSourcing and try not to make any assumptions on what domain classes you need and only create one when multiple events have hinted at one.

## Magnum Sal rules
It does!

But all shits 'n giggles aside, here are [the condensed rules](./condensed-rules.md), if anything's unclear here is [the actual rulebook](./rulebook.pdf).

## The kata rules
Make sure no impossible states can occur according to the Magnum Sal rules.  
Postpone creating domain classes as long as possible, purely rely on the `EventStream` instead.

## Tips
Start with starting a game with at least two players.  
Try to stall creating domain classes until you've implemented the _chain rule_.

# Learnings!
Thin line between making data classes with specific rules, and not creating domain objects.  
Value objects are ok to immediately write? Because they don't depend on that much state?

Kotlin has a nice feature called _lazy getters_ that can just delegate to functions on the EventStream:
```kotlin
private val players
        get() = eventStream.filterIsInstance<PlayerJoined>()
```

Depending on the use case you can postpone creating state for a veeeeery long time. We added a good use case to the kata's main page.
