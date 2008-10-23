/*
 * $Id$
 *
 * Copyright 2007-2008
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison,
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 *
 * http://www.ssec.wisc.edu/mcidas
 *
 * This file is part of McIDAS-V.
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package edu.wisc.ssec.mcidasv.util;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

public class McVTextField extends JTextField {
	
	public static char[] mcidasDeny = new char[] { '/', '.', ' ', '[', ']', '%' };
	
	public static char[] digitAllow = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.' };

	
	McVTextFieldDocument document = new McVTextFieldDocument();
	
	public McVTextField() {
		super();
		this.document = new McVTextFieldDocument(0, false, null, null);
		super.setDocument(document);
	}
	
	public McVTextField(String defaultString) {
		super();
		this.document = new McVTextFieldDocument(0, false, null, null);
		super.setDocument(document);
		this.setText(defaultString);
	}
	
	public McVTextField(String defaultString, int limit) {
		super(limit);
		this.document = new McVTextFieldDocument(limit, false, null, null);
		super.setDocument(document);
		this.setText(defaultString);
	}
	
	public McVTextField(String defaultString, boolean upper) {
		super();
		this.document = new McVTextFieldDocument(0, upper, null, null);
		super.setDocument(document);
		this.setText(defaultString);
	}
	
	public McVTextField(String defaultString, int limit, boolean upper) {
		super(limit);
		this.document = new McVTextFieldDocument(limit, upper, null, null);
		super.setDocument(document);
		this.setText(defaultString);
	}
	
	public McVTextField(String defaultString, int limit, boolean upper, char[] allow, char[] deny) {
		super(limit);
		this.document = new McVTextFieldDocument(limit, upper, allow, deny);
		super.setDocument(document);
		this.setText(defaultString);
	}

	public int getLimit() {
		return this.document.getLimit();
	}
	
	public void setLimit(int limit) {
		this.document.setLimit(limit);
		super.setDocument(document);
	}
	
	public boolean getUppercase() {
		return this.document.getUppercase();
	}
	
	public void setUppercase(boolean uppercase) {
		this.document.setUppercase(uppercase);
		super.setDocument(document);
	}
		
	public void setAllow(char[] characters) {
		this.document.setAllow(characters);
		super.setDocument(document);
	}
	
	public void setDeny(char[] characters) {
		this.document.setDeny(characters);
		super.setDocument(document);
	}
	
	private class McVTextFieldDocument extends PlainDocument {
		private int limit;
		private boolean toUppercase = false;
		private Hashtable charsAllow = new Hashtable();
		private Hashtable charsDeny = new Hashtable();
										
		public McVTextFieldDocument() {
			this(0, false, null, null);
		}
		
		public McVTextFieldDocument(int limit, boolean upper, char[] allow, char[] deny) {
			super();
			setLimit(limit);
			setUppercase(upper);
			if (allow!=null) setAllow(allow);
			if (deny!=null) setDeny(deny);
		}

		public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
			if (str == null) return;
			if (toUppercase) str = str.toUpperCase();
			
			System.out.println("insertString: " + str);
			
			// Only allow certain characters
			if (charsAllow.size()>0 || charsDeny.size()>0) {
				char[] characters = new char[str.length()];
				String okString = "";
				str.getChars(0, str.length(), characters, 0);
				for (char c : characters) {
					System.err.println("verifying: " + c);
					if (charsDeny.contains(c)) continue;
					if (charsAllow.size()<=0 || charsAllow.contains(c)) okString+=c;
				}
				str = okString;
			}
			System.out.println("verified: " + str);
			if (str.equals("")) return;

			if ((getLength() + str.length()) <= limit || limit <= 0) {
				super.insertString(offset, str, attr);
			}
		}
		
		public int getLimit() {
			return this.limit;
		}
		
		public void setLimit(int limit) {
			this.limit = limit;
		}
		
		public boolean getUppercase() {
			return this.toUppercase;
		}
		
		public void setUppercase(boolean uppercase) {
			this.toUppercase = uppercase;
		}
			
		public void setAllow(char[] characters) {
			Hashtable ht = new Hashtable();
			for (char c : characters) {
				ht.put(c, c);
			}
			this.charsAllow = ht;
		}
		
		public void setDeny(char[] characters) {
			Hashtable ht = new Hashtable();
			for (char c : characters) {
				ht.put(c, c);
			}
			this.charsDeny = ht;
		}

	}
}
