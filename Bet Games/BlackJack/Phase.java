// The moments of a round. The window uses it to know which buttons to enable.
public enum Phase {
    BETTING,     // waiting for the bets
    INSURANCE,   // the dealer shows an Ace: insurance offer
    PLAYING,     // the player is playing a hand
    ROUND_OVER   // the round ended and the results are on the screen
}