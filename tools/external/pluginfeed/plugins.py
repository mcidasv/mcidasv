import os
import datetime
import PyRSS2Gen as RSS2

# Please see plugin_desc_example.txt for an example plugin description file.

# Points to the directory where all plugin description files live.
# It is a good idea to keep the description files and plugins separate.
#DESCRIPTION_DIRECTORY = '/Users/jbeavers/rsstest/descs'
DESCRIPTION_DIRECTORY = ''

# Points to the directory that contains the actual plugins.
# Again, keep the description directory and plugin directory separate.
#PLUGIN_DIRECTORY = '/Users/jbeavers/rsstest/plugins'
PLUGIN_DIRECTORY = ''

# The filename that will eventually store our RSS data.
#RSS_FILE = '/Users/jbeavers/rsstest/test.rss'
RSS_FILE = ''

# The URL to the directory where the plugins live. Leave off the trailing '/'.
#WWW_PREFIX = 'http://www/mcidas/v'
WWW_PREFIX = ''

# The title of the feed. This is used so that we still [kinda] conform to RSS2
#FEED_TITLE = 'SSEC McIDAS-V Plugins'
FEED_TITLE = ''

# A link back to the feed. Again, for conformance.
#FEED_LINK = 'http://www/mcidas/v/%s' % (RSS_FILE)
FEED_LINK = ''

# A brief description of the feed.
#FEED_DESCRIPTION = 'Default list of SSEC McV plugins'
FEED_DESCRIPTION = ''

class McVRSSItem(RSS2.RSSItem):
	def __init__(self,
                 title,
                 link,
                 description,
                 pubDate,
                 category,
                 guid,

                 version = None):

		RSS2.RSSItem.__init__(self, title=title, link=link, description=description, pubDate=pubDate, categories=[category], guid=guid)
		self.version = version
		
	def publish_extensions(self, handler):
		"""
		Overrides RSSItem.publish_extension so that I can provide a 
		mcv:updated tag if needed.
		"""
		if self.version != None:
			RSS2._opt_element(handler, "mcv:version", self.version)

def read_descriptions(directory):
	"""
	Iterate through all of the files in the given directory and attempt to
	read plugin descriptions from each. This will return a dictionary of lists
	with the key being the name of the plugin. Each list in a dictionary entry
	will correspond to different versions of a given plugin.
	"""
	descriptions = {}
	for description in os.listdir(directory):
		fname = '%s/%s' % (directory, description)
		current = {}
		for line in open(fname).readlines():
			# ignore comments
			if line[0] == '#':
				print 'si!'
				continue

			(key, value) = line.split('=')
			current[key] = value.strip()
		
		try:
			descriptions[current['title']].append(current)
		except KeyError:
			descriptions[current['title']] = [current]

	return descriptions

def poll_mtimes(directory):
	"""
	Work through the plugin directory and accumulate mtime timestamps for each plugin.
	"""
	plugins = {}
	for plugin in os.listdir(directory):
		statinfo = os.stat('%s/%s' % (directory, plugin))
		plugins[plugin] = statinfo.st_mtime

	return plugins

def create_feed_items(feed):
	"""
	Iterates through all feed items and eventually returns a list of 
	McVRSSItem objects that correspond to each plugin.
	"""
	items = []
	for plugin in feed.keys():
		attrs = feed[plugin]
		item = McVRSSItem(
		    title = attrs['title'],
		    link = '%s/%s' % (WWW_PREFIX, attrs['link']),
		    description = attrs['description'],
		    pubDate = str(datetime.datetime.utcfromtimestamp(attrs['time'])),
		    category = attrs['category'],
		    guid = '%s/%s' % (WWW_PREFIX, attrs['link']),
			version = attrs['version']
			)
		items.append(item)
	return items	

def create_rss(fname, feed):
	"""
	Uses PyRSS2Gen to generate a RSS feed based upon the information passed
	in the feed parameter. Writes out the RSS to the file name specified by 
	fname.
	"""
	items = create_feed_items(feed)
	rss = RSS2.RSS2(
	    title = FEED_TITLE,
	    link = FEED_LINK,
	    description = FEED_DESCRIPTION,
	    lastBuildDate = datetime.datetime.now(),
	    items = items
	)

	rss.write_xml(open(fname, 'w+'))

def main():
	# first read all of the plugin description files
	descriptions = read_descriptions(DESCRIPTION_DIRECTORY)
	
	# now figure out when each plugin was last updated (for RSS conformance)
	current_mtimes = poll_mtimes(PLUGIN_DIRECTORY)
	
	# loop through all versions of each plugin and add it to the feed data
	feed = {}
	for plugin in descriptions.keys():
		for version in descriptions[plugin]:
			plugin_file = version['link']

			feed[plugin_file] = version
			
			# be sure to store the mtimes for the RSS pubDate tag
			feed[plugin_file]['time'] = current_mtimes[plugin_file]

	# write out the RSS representation of the feed data
	create_rss(RSS_FILE, feed)

if __name__ == '__main__':
	main()