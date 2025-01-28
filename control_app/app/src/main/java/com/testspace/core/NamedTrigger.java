package com.testspace.core;

/**
 * Version of trigger with caption.
 */
public class NamedTrigger implements Trigger {
    private final String caption;
    private final Trigger trigger;

    public NamedTrigger(String caption, Trigger trigger) {
        this.caption = caption;
        this.trigger = trigger;
    }

    @Override
    public void pull() {
        trigger.pull();
    }

    public final String getCaption() {
        return caption;
    }
}
