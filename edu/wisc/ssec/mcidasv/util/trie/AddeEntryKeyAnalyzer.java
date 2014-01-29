/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2014
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
 * 
 * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
 * some McIDAS-V source code is based on IDV and VisAD source code.  
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
package edu.wisc.ssec.mcidasv.util.trie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.wisc.ssec.mcidasv.servermanager.AddeEntry;
import edu.wisc.ssec.mcidasv.servermanager.EntryStore;
import edu.wisc.ssec.mcidasv.util.trie.PatriciaTrie.KeyAnalyzer;


public class AddeEntryKeyAnalyzer implements KeyAnalyzer<AddeEntry> {

    final static Logger logger = LoggerFactory.getLogger(EntryStore.class);
    
    // AddeEntrys are organized like so:
    // server->dataset->type->name
    // rets->rets3->IMAGE->
    @Override public int bitIndex(AddeEntry key, int keyStart, int keyLength, AddeEntry found, int foundStart, int foundLength) {
        
        if (found == null) {
            logger.debug("bitIndex: key={}; null found: ret={}", key.asStringId(), keyStart);
            return keyStart;
        }
        
        boolean allNull = true;
        logger.debug("bitIndex: key={} keyStart={} keyLen={} found={} foundStart={} foundLen={}", new Object[] { key.asStringId(),keyStart,keyLength,found.asStringId(),foundStart,foundLength});
        int length = Math.max(keyLength, foundLength);
        for (int i = 0; i < length; i++) {
            String fromKey = valAtIdx(i, key);
            String fromFound = valAtIdx(i, found);
            if (!fromKey.equals(fromFound)) {
                logger.debug("  diff: idx={} key={} found={}", new Object[] { i, key.asStringId(), found.asStringId()});
                return i;
            }
        }
        logger.debug("  equals!");
        return KeyAnalyzer.EQUAL_BIT_KEY;
    }

    // 
    @Override public int bitsPerElement() {
        return 1;
    }

    @Override public boolean isBitSet(AddeEntry key, int keyLength, int bitIndex) {
        return true;
    }

    @Override public boolean isPrefix(AddeEntry prefix, int offset, int length, AddeEntry key) {
        logger.debug("isPrefix: prefix={} offset={} len={} key={}", new Object[] { prefix.asStringId(), offset, length, key.asStringId() });
        for (int i = offset; i < length; i++) {
            String fromKey = valAtIdx(i, key);
            String fromPrefix = valAtIdx(i, prefix);
            if (!fromKey.equals(fromPrefix))
                return false;
        }
        return true;
    }

    @Override public int length(AddeEntry key) {
        return 4;
    }

    @Override public int compare(AddeEntry o1, AddeEntry o2) {
        return o1.asStringId().compareTo(o2.asStringId());
    }

    private static String valAtIdx(final int idx, final AddeEntry e) {
        switch (idx) {
            case 0: return e.getAddress();
            case 1: return e.getGroup();
            case 2: return e.getEntryType().name();
            case 3: return e.getName();
            default: throw new AssertionError();
        }
    }

}
