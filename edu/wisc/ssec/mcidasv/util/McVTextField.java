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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * Extend JTextField to add niceties such as uppercase,
 * length limits, and allow/deny character sets
 */
public class McVTextField extends JTextField {
	
	public static char[] mcidasDeny = new char[] { '/', '.', ' ', '[', ']', '%' };
			
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
	
	public McVTextField(String defaultString, int limit, boolean upper, String allow, String deny) {
		super(limit);
		this.document = new McVTextFieldDocument(limit, upper, makePattern(allow), makePattern(deny));
		super.setDocument(document);
		this.setText(defaultString);
	}
	
	public McVTextField(String defaultString, int limit, boolean upper, char[] allow, char[] deny) {
		super(limit);
		this.document = new McVTextFieldDocument(limit, upper, makePattern(allow), makePattern(deny));
		super.setDocument(document);
		this.setText(defaultString);
	}
	
	public McVTextField(String defaultString, int limit, boolean upper, Pattern allow, Pattern deny) {
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
		this.document.setAllow(makePattern(characters));
		super.setDocument(document);
	}
	
	public void setDeny(char[] characters) {
		this.document.setDeny(makePattern(characters));
		super.setDocument(document);
	}
	
	public void setAllow(Pattern newPattern) {
		this.document.setAllow(newPattern);
		super.setDocument(document);
	}
	
	public void setDeny(Pattern newPattern) {
		this.document.setDeny(newPattern);
		super.setDocument(document);
	}
	
	// Take a string and turn it into a pattern
	private Pattern makePattern(String string) {
		if (string == null) return null;
		try {
			return Pattern.compile(string);
		}
		catch (PatternSyntaxException e) {
			return null;
		}
	}
	
	// Take a character array and turn it into a [abc] class pattern
	private Pattern makePattern(char[] characters) {
		if (characters == null) return null;
		String string = ".*";
		if (characters.length > 0) {
			string = "[";
			for (char c : characters) {
				if (c == '[') string += "\\[";
				else if (c == ']') string += "\\]";
				else if (c == '\\') string += "\\\\";
				else string += c;
			}
			string += "]";
		}
		try {
			return Pattern.compile(string);
		}
		catch (PatternSyntaxException e) {
			return null;
		}
	}
	
	private class McVTextFieldDocument extends PlainDocument {
		private int limit;
		private boolean toUppercase = false;		
		private boolean hasPatterns = false;
		private Pattern allow = Pattern.compile(".*");
		private Pattern deny = null;
										
		public McVTextFieldDocument() {
			super();
		}
				
		public McVTextFieldDocument(int limit, boolean upper, Pattern allowPattern, Pattern denyPattern) {
			super();
			setLimit(limit);
			setUppercase(upper);
			if (allowPattern!=null) setAllow(allowPattern);
			if (denyPattern!=null) setDeny(denyPattern);
		}

		public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
			if (str == null) return;
			if (toUppercase) str = str.toUpperCase();
						
			// Only allow certain patterns, and only check if we think we have patterns
			if (hasPatterns) {
				char[] characters = str.toCharArray();
				String okString = "";
				for (char c : characters) {
					String s = "" + c;
					if (deny != null) {
						Matcher denyMatch = deny.matcher(s);
						if (denyMatch.matches()) continue;
					}
					if (allow != null) {
						Matcher allowMatch = allow.matcher(s);
						if (allowMatch.matches()) okString += s;
					}
				}
				str = okString;
			}
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
					
		public void setAllow(Pattern newPattern) {
			if (newPattern==null) return;
			this.allow = newPattern;
			hasPatterns = true;
		}
		
		public void setDeny(Pattern newPattern) {
			if (newPattern==null) return;
			this.deny = newPattern;
			hasPatterns = true;
		}
		
	}
}
