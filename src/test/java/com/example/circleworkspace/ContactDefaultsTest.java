package com.example.circleworkspace;

import org.junit.jupiter.api.Test;

import static com.example.circleworkspace.Model.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ContactDefaultsTest {
    @Test void newContactsDefaultToNoOverlap() {
        var contact = new ContactState(1, 1, 2, Tangency.EXTERNAL, 90, 270);
        assertEquals(ContactFollowMode.NO_OVERLAP_SWITCH_CONTACT, contact.followMode());
    }
}
