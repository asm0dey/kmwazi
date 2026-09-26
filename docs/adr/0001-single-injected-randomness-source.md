# One injected randomness source

Fairness is a product guarantee, so every random choice in `shared` goes through a single `Random` passed in from the platform shell (`SecureRandom` on Android). Nothing in `shared` may use `kotlin.random.Random.Default` or `java.security`; ArchUnit enforces both. The simpler global `Random` was rejected because a randomiser that is quietly predictable is broken, and the rule costs one test.
