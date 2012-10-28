package com.lukesegars.heatwave;

import java.util.Comparator;

public class ContactComparator implements Comparator<Contact> {
	public int compare(Contact lhs, Contact rhs) {
		return (lhs.getName().compareTo(rhs.getName()));
	}
}
