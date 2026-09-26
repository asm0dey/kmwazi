# Kmwazi

A multi-touch randomiser: players put fingers on the screen and the app picks a winner, splits them into groups, or puts them in order.

## Language

### Round

**Round**:
One cycle from the first finger touching down to everyone lifting after a Result.
_Avoid_: Game, session

**Finger**:
One touch point on the screen, standing for one player during a Round.
_Avoid_: Pointer, touch, player

**Colour**:
The colour a Finger is drawn in; a new Finger takes the lowest colour not held by a Finger still down.
_Avoid_: Color index, slot

**Countdown**:
The wait before the Draw; it runs only while at least two Fingers are down, and restarts whenever a Finger joins or leaves, but not when one moves.
_Avoid_: Stabilization, decision, armed

**Draw**:
The random choice that turns the Fingers down at the end of the Countdown into a Result.
_Avoid_: Deal, pick, reveal

**Timeout**:
The user-chosen length of the Countdown, 1–10 seconds.
_Avoid_: Decision timeout, delay

**Locked**:
A Round that already has a Result; Fingers may move or join but the Result stays.
_Avoid_: Frozen, done

### Modes

**Mode**:
What a Round produces: Choose One, Groups or Order.
_Avoid_: Game type

**Choose One**:
The Mode that picks a single Finger.

**Groups**:
The Mode that splits Fingers into groups of Group size; the last group takes the leftovers, even if that is a single Finger.
_Avoid_: Teams

**Order**:
The Mode that puts every Finger in a numbered sequence.
_Avoid_: DefineOrder (legacy stored value only)

**Group size**:
The remembered number of Fingers per group, 2–10; kept even while another Mode is selected.

**Result**:
What a Round produced: the winner, the groups, or the order.
_Avoid_: Outcome, pick
