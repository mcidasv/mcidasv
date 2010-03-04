package edu.wisc.ssec.mcidasv.util.trie;

import java.util.List;

import edu.wisc.ssec.mcidasv.util.trie.PatriciaTrie.KeyAnalyzer;


public class StringListKeyAnalyzer implements KeyAnalyzer<List<String>> {

    @Override public int bitIndex(List<String> key, int keyStart, int keyLength, List<String> found, int foundStart, int foundLength) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override public int bitsPerElement() {
        // TODO Auto-generated method stub
        return 1;
    }

    @Override public boolean isBitSet(List<String> key, int keyLength, int bitIndex) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override public boolean isPrefix(List<String> prefix, int offset, int length, List<String> key) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int length(List<String> key) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override public int compare(List<String> o1, List<String> o2) {
        // TODO Auto-generated method stub
        return 0;
    }

}
