import os
import datetime
import PyRSS2Gen as RSS2

# Points to the directory where all plugin description files live.
# It is a good idea to keep the description files and plugins separate.
DESCRIPTION_DIRECTORY = ''

# Points to the directory that contains the actual plugins.
# Again, keep the description directory and plugin directory separate.
PLUGIN_DIRECTORY = ''

# A file that will store the list of plugins that have been seen so far. It'll
# also be used to determine if a plugin has been updated since the last run
# of this script.
KNOWN_PLUGINS = ''

# The filename that will eventually store our RSS data.
RSS_FILE = ''

# The URL to the directory where the plugins live. Leave off the trailing '/'.
WWW_PREFIX = ''

# The title of the feed. This is used so that we still [kinda] conform to RSS2
FEED_TITLE = ''

# A link back to the feed. Again, for conformance.
FEED_LINK = ''

# Umm...
FEED_DESCRIPTION = ''

class McVRSSItem(RSS2.RSSItem):
	def __init__(self,
                 title,
                 link,
                 description,
                 pubDate,
                 category,
                 guid,

                 updated = None):

		RSS2.RSSItem.__init__(self, title=title, link=link, description=description, pubDate=pubDate, categories=[category], guid=guid)
		self.updated = updated
		
	def publish_extensions(self, handler):
		"""
		Overrides RSSItem.publish_extension so that I can provide a 
		mcv:updated tag if needed.
		"""
		if self.updated != None:
			RSS2._opt_element(handler, "mcv:updated", self.updated)

"""
Current item structure:
<item>
  <title>Workshop McV</title>
  <link>http://www.ssec.wisc.edu/mcidas/software/v/plugins/workshop.blah</link>
  <description>Workshop-specific configuration for McV</description>
  <category>Misc</category>
  <guid isPermaLink="false">http://www.ssec.wisc.edu/mcidas/software/v/plugins/workshop.blah</guid>
  <mcv:updated>1337</mcv:updated>
  <pubDate>blah blah</pubDate>
</item>

Issue: picking up new plugins or modifications:
so I work thru plugin dir and find mtimes.
then I work through a text file that has mtimes and plugins

if plugin from directory isn't in the text file, it's added to the feed
if plugin from directory has a newer mtime, it's added to the feed

mcv will have to be smart enough to signal new or updated plugins
I can just provide an extra tag similar to what LJ does: <mcv:updated>timestamp</mcv:updated>
mcv:updated will merely be the timestamp of the last update. there will never be more than
one mcv:updated per item. pubDate will always match the latest mtime.

Issue: what if a plugin is removed?
if a plugin wasn't found in the directory, but was in the file, remove from feed
mcv will have to be smart enough to know to remove it from the list of plugins

Issue: what if the feed gets huge?
the feed should contain all available plugins--this is the only approach
"""



def read_description(file):
	"""
	Reads a given description file and returns a dictionary containing the relevant data.
	"""
	description = {}
	for line in open(file).readlines():
		(key, value) = line.split('=')
		description[key] = value.strip()
	return description

def list_descriptions(directory):
	"""
	Return a filtered list of description files. Basically just filters out '.' and '..'.
	"""
	newlist = []
	for file in os.listdir(directory):
		if file != '.' and file != '..':
			newlist.append(file)
			
	return newlist

def poll_plugins(directory):
	"""
	Work through the plugin directory and accumulate mtime timestamps for each plugin.
	"""
	plugins = {}
	for plugin in os.listdir(directory):
		if plugin != '.' and plugin != '..':
			statinfo = os.stat('%s/%s' % (directory, plugin))
			plugins[plugin] = statinfo.st_mtime
			
	return plugins

def read_plugin_list(file):
	"""
	Read the plugins that have already been placed in the RSS feed. 
	Return the plugins and their last-observed mtimes.
	"""
	plugins = {}
	try:
		for line in open(file).readlines():
			(plugin, mtime) = line.strip().split(':')
			plugins[plugin] = float(mtime)
	except IOError:
		pass
		
	return plugins

def write_plugin_list(file, plugin_feed):
	try:
		f = open(file, 'w+')
		for plugin in plugin_feed.keys():
			f.write('%s:%s\n' % (plugin, plugin_feed[plugin]['time']))
			#print 'Plugin: %s mtime=%s' % (plugin, plugin_feed[plugin]['time'])
		f.close()
	except:
		print 'Error writing plugins!!'

def create_feed_items(data):
	items = []
	for plugin in data.keys():
		attrs = data[plugin]
		if attrs['updated'] == True:
			item = McVRSSItem(
			    title = attrs['title'],
			    link = '%s/%s' % (WWW_PREFIX, attrs['plugin']),
			    description = attrs['desc'],
			    pubDate = str(datetime.datetime.utcfromtimestamp(attrs['time'])),
			    updated = str(datetime.datetime.utcfromtimestamp(attrs['time'])),
			    category = attrs['category'],
			    guid = '%s/%s' % (WWW_PREFIX, attrs['plugin']),
			)
		else:
			item = McVRSSItem(
			    title = attrs['title'],
			    link = '%s/%s' % (WWW_PREFIX, attrs['plugin']),
			    description = attrs['desc'],
			    pubDate = str(datetime.datetime.utcfromtimestamp(attrs['time'])),
			    category = attrs['category'],
			    guid = '%s/%s' % (WWW_PREFIX, attrs['plugin']),
			)
		items.append(item)
	return items

def create_rss(file, feed):
	items = create_feed_items(feed)
	rss = RSS2.RSS2(
	    title = FEED_TITLE,
	    link = FEED_LINK,
	    description = FEED_DESCRIPTION,
	    lastBuildDate = datetime.datetime.now(),
	
	    items = items
	)

	rss.write_xml(open(file, 'w+'))


def main():
	"""
	The main function sifts through all of the various files and information to build
	the RSS file containing all of our plugins.
	"""
	descriptions = {}
	for file in list_descriptions(DESCRIPTION_DIRECTORY):
		desc = read_description('%s/%s' % (DESCRIPTION_DIRECTORY, file))
		descriptions[desc['plugin']] = desc
	
	old_plugins = read_plugin_list(KNOWN_PLUGINS)
	current_mtimes = poll_plugins(PLUGIN_DIRECTORY)
	
	feed = {}
	for plugin in descriptions.keys():
		# if the plugin has already been observed we need to determine if it 
		# has been updated.
		if plugin in old_plugins.keys():
			feed[plugin] = descriptions[plugin]
			
			if current_mtimes[plugin] > old_plugins[plugin]:
				# looks like there's been an update
				feed[plugin]['updated'] = True
				feed[plugin]['time'] = current_mtimes[plugin]
			else:
				# no update, just use "old" timestamp
				feed[plugin]['updated'] = False
				feed[plugin]['time'] = old_plugins[plugin]
		else:
			# haven't seen this plugin, add it to the feed!
			feed[plugin] = descriptions[plugin]
			feed[plugin]['time'] = current_mtimes[plugin]
			feed[plugin]['updated'] = False
	
	create_rss(RSS_FILE, feed)
	
	write_plugin_list(KNOWN_PLUGINS, feed)

if __name__ == '__main__':
	main()