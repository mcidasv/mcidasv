/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2009
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
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
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package edu.wisc.ssec.mcidasv.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
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
	
	public static Pattern ipAddress = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");

	private McVTextFieldDocument document = new McVTextFieldDocument();
	
	private Pattern validPattern;
	private String[] validStrings;
	
	public McVTextField() {
		this("", 0, false);
	}
	
	public McVTextField(String defaultString) {
		this(defaultString, 0, false);
	}
	
	public McVTextField(String defaultString, int limit) {
		this(defaultString, limit, false);
	}
	
	public McVTextField(String defaultString, boolean upper) {
		this(defaultString, 0, upper);
	}
	
	// All other constructors call this one
	public McVTextField(String defaultString, int limit, boolean upper) {
		super(limit);
		this.document = new McVTextFieldDocument(limit, upper);
		super.setDocument(document);
		this.setText(defaultString);
	}
	
	public McVTextField(String defaultString, int limit, boolean upper, String allow, String deny) {
		this(defaultString, limit, upper);
		setAllow(makePattern(allow));
		setDeny(makePattern(deny));
	}
	
	public McVTextField(String defaultString, int limit, boolean upper, char[] allow, char[] deny) {
		this(defaultString, limit, upper);
		setAllow(makePattern(allow));
		setDeny(makePattern(deny));
	}
	
	public McVTextField(String defaultString, int limit, boolean upper, Pattern allow, Pattern deny) {
		this(defaultString, limit, upper);
		setAllow(allow);
		setDeny(deny);
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
		
	public void setAllow(String string) {
		this.document.setAllow(makePattern(string));
		super.setDocument(document);
	}
	
	public void setDeny(String string) {
		this.document.setDeny(makePattern(string));
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
	
	// Add an InputVerifier if we want to validate a particular pattern
	public void setValidPattern(String string) {
		if (string == null) return;
		try {
			Pattern newPattern = Pattern.compile(string);
			setValidPattern(newPattern);
		}
		catch (PatternSyntaxException e) {
			return;
		}
	}
	
	// Add an InputVerifier if we want to validate a particular pattern
	public void setValidPattern(Pattern pattern) {
		if (pattern == null) {
			this.validPattern = null;
			if (this.validStrings == null) removeInputVerifier();
		}
		else {
			this.validPattern = pattern;
			addInputVerifier();
		}
	}
	
	// Add an InputVerifier if we want to validate a particular set of strings
	public void setValidStrings(String[] strings) {
		if (strings == null) {
			this.validStrings = null;
			if (this.validPattern == null) removeInputVerifier();
		}
		else {
			this.validStrings = strings;
			addInputVerifier();
		}
	}
	
	private void addInputVerifier() {
		this.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent comp) {
				return verifyInput();
			}
			public boolean shouldYieldFocus(JComponent comp) {
				boolean valid = verify(comp);
				if (!valid) {
					getToolkit().beep();
				}
				return valid;
			}
		});
		verifyInput();
	}
	
	private void removeInputVerifier() {
		this.setInputVerifier(null);
	}
	
	private boolean verifyInput() {
		boolean isValid = false;
		String checkValue = this.getText();
		if (checkValue=="") return true;
		
		if (this.validStrings != null) {
			for (String string : validStrings) {
				if (checkValue.equals(string)) isValid = true;
			}
		}
		
		if (this.validPattern != null) {
			Matcher validMatch = this.validPattern.matcher(checkValue);
			isValid = isValid || validMatch.matches();
		}
		
		if (!isValid) {
			this.selectAll();
		}
		return isValid;
	}
	
	/**
	 * Extend PlainDocument to get the character validation features we require
	 */
	private class McVTextFieldDocument extends PlainDocument {
		private int limit;
		private boolean toUppercase = false;		
		private boolean hasPatterns = false;
		private Pattern allow = Pattern.compile(".*");
		private Pattern deny = null;
										
		public McVTextFieldDocument() {
			super();
		}
				
		public McVTextFieldDocument(int limit, boolean upper) {
			super();
			setLimit(limit);
			setUppercase(upper);
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
