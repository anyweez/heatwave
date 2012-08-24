package com.lukesegars.heatwave.exceptions;

/**
 * This exception is raised when invalid details (such as an Android ID) are 
 * provided for a contact.
 * 
 * Example: if an Android ID is provided to @link HeatwaveDatabase.fetchContact()
 * but does not refer to a valid record then this exception will be raised instead.
 */
public class ContactNotFoundException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
