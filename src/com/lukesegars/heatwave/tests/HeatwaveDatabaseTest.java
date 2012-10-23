package com.lukesegars.heatwave.tests;

import java.util.ArrayList;

import com.lukesegars.heatwave.Contact;
import com.lukesegars.heatwave.HeatwaveDatabase;

import android.test.AndroidTestCase;
import android.util.Log;

public class HeatwaveDatabaseTest extends AndroidTestCase {
	private HeatwaveDatabase db = null;
	private final String TAG = "com.lukesegars.heatwave.HeatwaveDatabaseTest";
	
	/**
	 * The number of contacts that have been added to the Heatwave database.
	 * This is NOT the number of contacts in the address book.
	 */
	private int NUM_CONTACTS = 4;
	
	@Override
	protected void setUp() throws Exception {
		db = HeatwaveDatabase.getInstance(getContext());
		
		super.setUp();
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * Check all of the preconditions for the test.  If this doesn't pass
	 * then something is very wrong, likely with initializing the database.
	 */
	public void testPreconditions() {
		assertNotNull(db);
	}
	
	/**
	 * Reading contacts and making sure that all fields are set correctly.
	 */
	public void testReadContacts() {
		ArrayList<Contact> contacts = db.fetchContacts();
		ArrayList<Long> cids = new ArrayList<Long>();
		assertEquals(NUM_CONTACTS, contacts.size());
		
		// All contacts must have an ID and a name.  Wave and last call ID
		// are not required.
		for (Contact c : contacts) {
			// Build up the cids list for a future test.
			cids.add(c.getAdrId());
			assertTrue(c.getAdrId() > 0);
			assertNotNull(c.getName());
		}
		cids.add((long) 10000);
		
		// Check to make sure that requesting a non-existing user ID is OK.
		db.removeContact(contacts.get(0).getAdrId());
		
		assertNull(db.fetchContact(contacts.get(0).getAdrId()));
		assertNull(db.fetchContact(Math.round(Math.random() * 100) + 50000));
		assertEquals(false, db.contactExists(contacts.get(0).getAdrId()));
		
		db.addContact(contacts.get(0));
		
		ArrayList<Long> ids = db.getActiveContactAdrIds();
		// cids and ids should be identical lists.
		for (int i = 0; i < ids.size(); i++) {
			assertEquals(true, cids.contains(ids.get(i)));
			assertEquals(true, ids.contains(cids.get(i)));
		}
		
		// TODO: Test HWD.updateTimestamp()
	}
	
	/**
	 * Writing contacts.
	 */
	public void testWriteContacts() {
		// Test to make sure that adding a pre-existing contact doesn't change
		// the contact count.
		
		// TODO: Test HWD.addContacts()
		// TODO: Test HWD.updateContact()
		// TODO: Test HWD.removeContact()
		// TODO: Try removing a contact that doesn't exist.
		// TODO: Test HWD.removeContacts()
	}

	public void testReadWaves() {
		// TODO: Test HWD.fetchWave()
		// TODO: Test HWD.getWaveMemberCount()
		// TODO: Test HWD.loadWaveByName()
		// TODO: Test HWD.fetchWaves()
		// TODO: Test HWD.getPhoneForContact()
		// TODO: Test HWD.getSnoozeLog()
	}
	
	public void testWriteWaves() {
		// TODO: Test HWD.addWave()
		// TODO: Test HWD.removeWave()
		// TODO: Test HWD.updateWave()
		// TODO: Test HWD.addSnoozeRecord()
		// TODO: Test HWD.removeSnoozeRecord()
	}
}
