package com.mywehr.enums;

/**
 * Declarative wait intent used by ElementUtils, so page objects never have to
 * build ExpectedConditions by hand.
 */
public enum WaitStrategy {

    /** Element is in the DOM (may still be invisible). */
    PRESENT,

    /** Element is in the DOM and has a non-zero size. */
    VISIBLE,

    /** Element is visible and enabled. */
    CLICKABLE,

    /** Element has left the DOM or become invisible. */
    INVISIBLE,

    /** No wait at all - caller already knows the element is settled. */
    NONE
}
