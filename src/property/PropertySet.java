package property;

import java.util.HashSet;
import java.util.Set;

import lustre.LustreTrace;

/**
 * The set of properties that can be falsified by a test case. Assuming all
 * properties are trap properties.
 */
public final class PropertySet implements Comparable<PropertySet> {
	public final LustreTrace testCase;
	public final Set<LustreProperty> properties;

	public PropertySet(LustreTrace testCase) {
		this.testCase = testCase;
		this.properties = new HashSet<LustreProperty>();
	}

	@Override
	public int compareTo(PropertySet other) {
		if (this.properties.size() < other.properties.size()) {
			return 1;
		} else if (this.properties.size() == other.properties.size()) {
			return 0;
		} else {
			return -1;
		}
	}
}
