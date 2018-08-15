# Conversation

A simple no-assumptions, no-dependency library for writing reentrant stateful bots.

This library provides away to structure conversations, perhaps collect information
along the way, while not requiring a persistent thread or process between requests.

Fundamentally, this is providing a state machine with simple namespaces or `dialog
references`, and a built in way to index and continue the state machine on new 
messages--while contributing to the `context` state. The context is internally 
treated as an immutable value, so each state or `step` must return the
context--mutated or copied with modifications. A `Bot` is a reference to the whole
state machine, with a pointer to the first `Dialog`, each `Dialog` likewise has a
pointer to the first `Step`. Upon a new message given to a `Bot`, it indexes the
`Dialog` reference, defaulting to the `Bot`'s `rootDialog`, then indexes the `Step`
reference--a step is executed once per message. Steps cannot be chained within a
`converse` action. `Step` implementations take a `context` and a `message` and are
expected to return a `ConverseResult` of the new context (may be the same by
reference mutated, or a new value of the same type), the next `Dialog` reference,
and the next `Step` reference. `Dialog` and `Step` references are generically
typed to the `Conversation`. However, use of a `SimpleConversation` requires the
caller maintain and remember what the next `Dialog` and `Step` is with the
`ConverseResult` from `converse`. As a a helper, an interface is added for a
`MemorizingConversation` which uses a `MemorizingContext` such that the next
`Dialog` and next `Step` are persisted to the context such that `converse` takes
a `context` and a `message`. It will derive the `Dialog` and `Step` references
and then return just a `context`. Lastly, a `Builder` utility class exists which
can help create implementations using a fluent builder API.

It is up to the user to persist and provide the `context` for each incoming message.
Side effects may occur within the `Step` functions, or be added to the `context`
to be executed outside of the conversation. 

Read-oriented IO, such as downloading an attachment on a message, should be safe
to have inside of `Step` functions. However, do note that `context` should likely
include whatever implementations are necessary to do IO. Side effects such as
responding to the end user may be within or outside of the `Step` functions, so
long as the intent is serialized on the `context`. It would be unwise to use
multiple `Step` functions to accomplish an an action in response to a single
end user message.

---

I plan to use https://jitpack.io to package this for my other projects.

---

This library is designed after implementing lisp based state machine bots for
text games, using co-routines, actors, async await, and reviewing the bot
framework from Microsoft. (Note, I have not actually used the bot framework
from Microsoft.)

I plan to use it with Telegram, where the context is indexed by `chat.id` from
the outside.
