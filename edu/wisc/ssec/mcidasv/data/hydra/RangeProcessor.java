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

package edu.wisc.ssec.mcidasv.data.hydra;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.wisc.ssec.mcidasv.data.QualityFlag;

import visad.util.Util;

public class RangeProcessor {

	private static final Logger logger = LoggerFactory
			.getLogger(RangeProcessor.class);

	static RangeProcessor createRangeProcessor(MultiDimensionReader reader,
			HashMap metadata) throws Exception {
		if (reader instanceof GranuleAggregation) {
			return new AggregationRangeProcessor((GranuleAggregation) reader,
					metadata);
		}

		if (metadata.get("scale_name") == null) {
			String product_name = (String) metadata
					.get(SwathAdapter.product_name);
			if (product_name == "IASI_L1C_xxx") {
				return new IASI_RangeProcessor();
			}
			return null;
		} else {
			String product_name = (String) metadata
					.get(ProfileAlongTrack.product_name);
			if (product_name == "2B-GEOPROF") {
				return new CloudSat_2B_GEOPROF_RangeProcessor(reader, metadata);
			} else {
				return new RangeProcessor(reader, metadata);
			}
		}
	}

	MultiDimensionReader reader;
	HashMap metadata;

	float[] scale = null;
	float[] offset = null;
	float[] missing = null;
	float[] valid_range = null;
	float valid_low = -Float.MAX_VALUE;
	float valid_high = Float.MAX_VALUE;
	float[] low = new float[] { -Float.MAX_VALUE };
	float[] high = new float[] { Float.MAX_VALUE };

	boolean unpack = false;
	boolean unsigned = false;
	boolean rangeCheckBeforeScaling = true;

	int scaleOffsetLen = 1;

	String multiScaleDimName = SpectrumAdapter.channelIndex_name;
	boolean hasMultiDimensionScale = false;

	int multiScaleDimensionIndex = 0;

	int soIndex = 0;

	public RangeProcessor() {
	}

	public RangeProcessor(float scale, float offset, float valid_low,
			float valid_high, float missing) {
		this.scale = new float[] { scale };
		this.offset = new float[] { offset };
		this.missing = new float[] { missing };
		this.valid_low = valid_low;
		this.valid_high = valid_high;
	}

	public RangeProcessor(MultiDimensionReader reader, HashMap metadata,
			String multiScaleDimName) throws Exception {
		this(reader, metadata);
		this.multiScaleDimName = multiScaleDimName;
	}

	public RangeProcessor(MultiDimensionReader reader, HashMap metadata)
			throws Exception {
		this.reader = reader;
		this.metadata = metadata;

		if (metadata.get("unpack") != null) {
			unpack = true;
		}

		if (metadata.get("unsigned") != null) {
			unsigned = true;
		}

		if (metadata.get("range_check_after_scaling") != null) {
			String s = (String) metadata.get("range_check_after_scaling");
			logger.debug("range_check_after_scaling: " + s);
			rangeCheckBeforeScaling = false;
		}

		String array_name = (String) metadata.get("array_name");

		scale = getAttributeAsFloatArray(array_name,
				(String) metadata.get("scale_name"));

		offset = getAttributeAsFloatArray(array_name,
				(String) metadata.get("offset_name"));

		if (scale != null) {
			scaleOffsetLen = scale.length;

			if (offset != null) {
				if (scale.length != offset.length) {
					throw new Exception(
							"RangeProcessor: scale and offset array lengths must be equal");
				}
			} else {
				offset = new float[scaleOffsetLen];
				for (int i = 0; i < offset.length; i++)
					offset[i] = 0f;
			}

		}

		missing = getAttributeAsFloatArray(array_name,
				(String) metadata.get("fill_value_name"));

		String metaStr = (String) metadata.get("valid_range");
		// attr name not supplied, so try the convention default
		if (metaStr == null) {
			metaStr = "valid_range";
		}

		valid_range = getAttributeAsFloatArray(array_name, metaStr);
		if (valid_range != null) {

			valid_low = valid_range[0];
			valid_high = valid_range[1];

			if (valid_range[0] > valid_range[1]) {
				valid_low = valid_range[1];
				valid_high = valid_range[0];
			}
		}

		String str = (String) metadata.get("multiScaleDimensionIndex");
		hasMultiDimensionScale = (str != null);
		multiScaleDimensionIndex = (str != null) ? Integer.parseInt(str) : 0;
	}

	public float[] getAttributeAsFloatArray(String arrayName, String attrName)
			throws Exception {
		float[] fltArray = null;
		HDFArray arrayAttr = reader.getArrayAttribute(arrayName, attrName);

		if (arrayAttr != null) {

			if (arrayAttr.getType().equals(Float.TYPE)) {
				float[] attr = (float[]) arrayAttr.getArray();
				fltArray = new float[attr.length];
				for (int k = 0; k < attr.length; k++)
					fltArray[k] = attr[k];
			} else if (arrayAttr.getType().equals(Short.TYPE)) {
				short[] attr = (short[]) arrayAttr.getArray();
				fltArray = new float[attr.length];
				for (int k = 0; k < attr.length; k++)
					fltArray[k] = (float) attr[k];
			} else if (arrayAttr.getType().equals(Integer.TYPE)) {
				int[] attr = (int[]) arrayAttr.getArray();
				fltArray = new float[attr.length];
				for (int k = 0; k < attr.length; k++)
					fltArray[k] = (float) attr[k];
			} else if (arrayAttr.getType().equals(Double.TYPE)) {
				double[] attr = (double[]) arrayAttr.getArray();
				fltArray = new float[attr.length];
				for (int k = 0; k < attr.length; k++)
					fltArray[k] = (float) attr[k];
			}

		}

		return fltArray;
	}

	/**
	 * Process a range of data from a byte array where bytes are packed bit or
	 * multi-bit fields of quality flags. Based on info in a QualityFlag object
	 * passed in, we extract and return values for that flag.
	 * 
	 * @param values
	 *            input values
	 * @param subset
	 *            optional subset
	 * @param qf
	 *            quality flag
	 * @return processed range
	 */

	public float[] processRangeQualityFlag(byte[] values, HashMap subset,
			QualityFlag qf) {

		if (subset != null) {
			if (subset.get(multiScaleDimName) != null) {
				soIndex = (int) ((double[]) subset.get(multiScaleDimName))[0];
			}
		}

		float[] newValues = new float[values.length];

		float val = 0f;
		int bitOffset = qf.getBitOffset();
		int divisor = -1;

		// map bit offset to a divisor
		switch (bitOffset) {
		case 1:
			divisor = 2;
			break;
		case 2:
			divisor = 4;
			break;
		case 3:
			divisor = 8;
			break;
		case 4:
			divisor = 16;
			break;
		case 5:
			divisor = 32;
			break;
		case 6:
			divisor = 64;
			break;
		case 7:
			divisor = 128;
			break;
		default:
			divisor = 1;
			break;
		}

		// now map bit width to a mask
		int numBits = qf.getNumBits();
		int mask = -1;
		switch (numBits) {
		case 1:
			mask = (int) 0x00000001;
			break;
		case 2:
			mask = (int) 0x00000003;
			break;
		case 3:
			mask = (int) 0x00000007;
			break;
		case 4:
			mask = (int) 0x0000000F;
			break;
		case 5:
			mask = (int) 0x0000001F;
			break;
		case 6:
			mask = (int) 0x0000003F;
			break;
		case 7:
			mask = (int) 0x0000007F;
			break;
		default:
			mask = (int) 0x00000000;
			break;
		}

		int i = 0;
		for (int k = 0; k < values.length; k++) {
			val = (float) values[k];
			i = Util.unsignedByteToInt(values[k]);
			val = (float) ((i / divisor) & mask);
			newValues[k] = val;
		}

		return newValues;
	}

	/**
	 * Process a range of data from a byte array
	 * 
	 * @param values
	 * @param subset
	 * @return
	 */

	public float[] processRange(byte[] values, HashMap subset) {

		if (subset != null) {
			if (subset.get(multiScaleDimName) != null) {
				soIndex = (int) ((double[]) subset.get(multiScaleDimName))[0];
			}
		}

		float[] new_values = new float[values.length];

		// if we are working with unsigned data, need to convert missing vals to
		// unsigned too
		if (unsigned) {
			if (missing != null) {
				for (int i = 0; i < missing.length; i++) {
					missing[i] = (float) Util.unsignedByteToInt((byte) missing[i]);
				}
			}
		}

		float val = 0f;
		int i = 0;
		boolean isMissing = false;

		for (int k = 0; k < values.length; k++) {

			val = (float) values[k];
			if (unsigned) {
				i = Util.unsignedByteToInt(values[k]);
				val = (float) i;
			}

			// first, check the (possibly multiple) missing values
			isMissing = false;
			if (missing != null) {
				for (int mvIdx = 0; mvIdx < missing.length; mvIdx++) {
					if (val == missing[mvIdx]) {
						isMissing = true;
						break;
					}
				}
			}

			if (isMissing) {
				new_values[k] = Float.NaN;
				continue;
			}

			if (rangeCheckBeforeScaling) {
				if ((val < valid_low) || (val > valid_high)) {
					new_values[k] = Float.NaN;
					continue;
				}
			}

			if (scale != null) {
				if (unpack) {
					new_values[k] = scale[soIndex] * (val) + offset[soIndex];
				} else {
					new_values[k] = scale[soIndex] * (val - offset[soIndex]);
				}
			} else {
				new_values[k] = val;
			}

			// do valid range check AFTER scaling?
			if (!rangeCheckBeforeScaling) {
				if ((new_values[k] < valid_low) || (new_values[k] > valid_high)) {
					new_values[k] = Float.NaN;
				}
			}
		}
		return new_values;
	}

	/**
	 * Process a range of data from a short array
	 * 
	 * @param values
	 * @param subset
	 * @return
	 */

	public float[] processRange(short[] values, HashMap subset) {

		if (subset != null) {
			if (subset.get(multiScaleDimName) != null) {
				soIndex = (int) ((double[]) subset.get(multiScaleDimName))[0];
			}
		}

		float[] new_values = new float[values.length];

		// if we are working with unsigned data, need to convert missing vals to
		// unsigned too
		if (unsigned) {
			if (missing != null) {
				for (int i = 0; i < missing.length; i++) {
					missing[i] = (float) Util.unsignedShortToInt((short) missing[i]);
				}
			}
		}

		float val = 0f;
		int i = 0;
		boolean isMissing = false;

		for (int k = 0; k < values.length; k++) {

			val = (float) values[k];
			if (unsigned) {
				i = Util.unsignedShortToInt(values[k]);
				val = (float) i;
			}

			// first, check the (possibly multiple) missing values
			isMissing = false;
			if (missing != null) {
				for (int mvIdx = 0; mvIdx < missing.length; mvIdx++) {
					if (val == missing[mvIdx]) {
						isMissing = true;
						break;
					}
				}
			}

			if (isMissing) {
				new_values[k] = Float.NaN;
				continue;
			}

			if (rangeCheckBeforeScaling) {
				if ((val < valid_low) || (val > valid_high)) {
					new_values[k] = Float.NaN;
					continue;
				}
			}

			if (scale != null) {
				if (unpack) {
					new_values[k] = (scale[soIndex] * val) + offset[soIndex];
				} else {
					new_values[k] = scale[soIndex] * (val - offset[soIndex]);
				}
			} else {
				new_values[k] = val;
			}

			// do valid range check AFTER scaling?
			if (!rangeCheckBeforeScaling) {
				if ((new_values[k] < valid_low) || (new_values[k] > valid_high)) {
					new_values[k] = Float.NaN;
				}
			}

		}
		return new_values;
	}

	/**
	 * Process a range of data from a float array
	 * 
	 * @param values
	 * @param subset
	 * @return
	 */

	public float[] processRange(float[] values, HashMap subset) {

		float[] new_values = null;

		if ((missing != null) || (valid_range != null)) {
			new_values = new float[values.length];
		} else {
			return values;
		}

		float val;

		for (int k = 0; k < values.length; k++) {
			val = values[k];
			new_values[k] = val;

			// first, check the (possibly multiple) missing values
			if (missing != null) {
				for (int mvIdx = 0; mvIdx < missing.length; mvIdx++) {
					if (val == missing[mvIdx]) {
						new_values[k] = Float.NaN;
						break;
					}
				}
			}

			if ((valid_range != null)
					&& ((val < valid_low) || (val > valid_high))) {
				new_values[k] = Float.NaN;
			}

		}

		return new_values;
	}

	/**
	 * Process a range of data from a double array
	 * 
	 * @param values
	 * @param subset
	 * @return
	 */

	public double[] processRange(double[] values, HashMap subset) {

		double[] new_values = null;

		if ((missing != null) || (valid_range != null)) {
			new_values = new double[values.length];
		} else {
			return values;
		}

		double val;

		for (int k = 0; k < values.length; k++) {
			val = values[k];
			new_values[k] = val;

			// first, check the (possibly multiple) missing values
			if (missing != null) {
				for (int mvIdx = 0; mvIdx < missing.length; mvIdx++) {
					if (val == missing[mvIdx]) {
						new_values[k] = Float.NaN;
						break;
					}
				}
			}

			if ((valid_range != null)
					&& ((val < valid_low) || (val > valid_high))) {
				new_values[k] = Double.NaN;
			}
		}

		return new_values;
	}

	/**
	 * Process a range of data from a byte array
	 * 
	 * @param values
	 * @return
	 */

	public float[] processAlongMultiScaleDim(byte[] values) {

		float[] new_values = new float[values.length];

		// if we are working with unsigned data, need to convert missing vals to
		// unsigned too
		if (unsigned) {
			if (missing != null) {
				for (int i = 0; i < missing.length; i++) {
					missing[i] = (float) Util.unsignedByteToInt((byte) missing[i]);
				}
			}
		}

		float val = 0f;
		int i = 0;
		boolean isMissing = false;

		for (int k = 0; k < values.length; k++) {

			val = (float) values[k];
			if (unsigned) {
				i = Util.unsignedByteToInt(values[k]);
				val = (float) i;
			}

			// first, check the (possibly multiple) missing values
			isMissing = false;
			if (missing != null) {
				for (int mvIdx = 0; mvIdx < missing.length; mvIdx++) {
					if (val == missing[mvIdx]) {
						isMissing = true;
						break;
					}
				}
			}

			if (isMissing) {
				new_values[k] = Float.NaN;
				continue;
			}

			if (rangeCheckBeforeScaling) {
				if ((val < valid_low) || (val > valid_high)) {
					new_values[k] = Float.NaN;
					continue;
				}
			}

			if (unpack) {
				new_values[k] = scale[k] * val + offset[k];
			} else {
				new_values[k] = scale[k] * (val - offset[k]);
			}

			// do valid range check AFTER scaling?
			if (!rangeCheckBeforeScaling) {
				if ((new_values[k] < valid_low) || (new_values[k] > valid_high)) {
					new_values[k] = Float.NaN;
				}
			}
		}
		return new_values;
	}

	/**
	 * Process a range of data from a short array
	 * 
	 * @param values
	 * @return
	 */

	public float[] processAlongMultiScaleDim(short[] values) {

		float[] new_values = new float[values.length];

		// if we are working with unsigned data, need to convert missing vals to
		// unsigned too
		if (unsigned) {
			if (missing != null) {
				for (int i = 0; i < missing.length; i++) {
					missing[i] = (float) Util.unsignedShortToInt((short) missing[i]);
				}
			}
		}

		float val = 0f;
		int i = 0;
		boolean isMissing = false;

		for (int k = 0; k < values.length; k++) {

			val = (float) values[k];
			if (unsigned) {
				i = Util.unsignedShortToInt(values[k]);
				val = (float) i;
			}

			// first, check the (possibly multiple) missing values
			isMissing = false;
			if (missing != null) {
				for (int mvIdx = 0; mvIdx < missing.length; mvIdx++) {
					if (val == missing[mvIdx]) {
						isMissing = true;
						break;
					}
				}
			}

			if (isMissing) {
				new_values[k] = Float.NaN;
				continue;
			}

			if (rangeCheckBeforeScaling) {
				if ((val < valid_low) || (val > valid_high)) {
					new_values[k] = Float.NaN;
					continue;
				}
			}

			if (unpack) {
				new_values[k] = scale[k] * val + offset[k];
			} else {
				new_values[k] = scale[k] * (val - offset[k]);
			}

			// do valid range check AFTER scaling?
			if (!rangeCheckBeforeScaling) {
				if ((new_values[k] < valid_low) || (new_values[k] > valid_high)) {
					new_values[k] = Float.NaN;
				}
			}
		}
		return new_values;
	}

	public void setMultiScaleDimName(String multiScaleDimName) {
		this.multiScaleDimName = multiScaleDimName;
	}

	public int getMultiScaleDimensionIndex() {
		return multiScaleDimensionIndex;
	}

	public boolean hasMultiDimensionScale() {
		return hasMultiDimensionScale;
	}

	public void setHasMultiDimensionScale(boolean yesno) {
		hasMultiDimensionScale = yesno;
	}

	public void setMultiScaleIndex(int idx) {
		this.soIndex = idx;
	}

}