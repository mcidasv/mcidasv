from __future__ import with_statement
from os import listdir

# Note: for the time being, do not remove icon_compass.png or any of the range-bearing icons.

# The path to actions.xml
# Example: ACTIONS = '/Users/jbeavers/Documents/mcv/mcv/edu/wisc/ssec/mcidasv/resources/actions.xml'
ACTIONS = ''

# The path to the McV icon sets.
# Example: ICON_PATH = '/Users/jbeavers/Documents/mcv/mcv/edu/wisc/ssec/mcidasv/resources/icons'
ICON_PATH = ''

def icon_filter(s):
	"""Determine if a string is a McV icon."""
	if 'image="/edu/wisc/ssec/mcidasv/resources/icons/' in s:
		return True
	return False

def clean_up(s):
	"""Remove excess junk from icon paths."""
	# quick hack. WHAT?! STOP JUDGING ME! :(
	return s.strip().replace('image="/edu/wisc/ssec/mcidasv/resources/icons/', '').replace('%d.png"', '').replace('32.png', '').replace('22.png', '').replace('16.png', '')

def report_orphans():
	"""Quick and dirty way to find icon sets that aren't being used but are still hanging around on disk."""
	with open(ACTIONS, 'r') as f:
		actions = map(clean_up, filter(icon_filter, f.readlines()))

		icons = set()
		[icons.add(x) for x in map(clean_up, listdir(ICON_PATH))]

		for icon in sorted(icons):
			if icon not in actions:
				print 'Likely orphan:', icon

if __name__ == '__main__':
	report_orphans()